# Koru

Automatically generates wrappers for `suspend` functions and `Flow` for easy access from Swift code in Kotlin Multiplatform projects.

Inspired by https://touchlab.co/kotlin-coroutines-rxswift/ by Russell Wolf.

**Note**: this is an **early preview**. Both the library api and maven coordinates are likely to change.

## Basic example

Let's say you have a class in the `shared` module, that looks like this:

```kotlin
@ToNativeClass(name = "LoadUserUseCaseIos")
class LoadUserUseCase(private val service: Service) {

    suspend fun loadUser(username: String) : User? = service.loadUser(username)
    
}
```

Such use case can be easily consumed from Android code, but in Kotlin Native (e.g. iOS) suspend functions generate a completion handler which is a bit of a PITA to work with.

When you add `@ToNativeClass` annotation to the class, a wrapper is generated:

```kotlin
public class LoadUserUseCaseIos(private val wrapped: LoadUserUseCase) {

  public fun loadUser(username: String): SuspendWrapper<User?> = 
      SuspendWrapper(null) { wrapped.loadUser(username) }
  
}
```

Notice that in place of `suspend` function, we get a function exposing `SuspendWrapper`. When you expose `LoadUserUseCaseIos` to your Swift code, it can be consumed like this:

```swift
loadUserUseCaseIos.loadUser(username: "foo").subscribe(
            scope: coroutineScope, //this can be provided automatically, more on that below
            onSuccess: { user in print(user?.description() ?? "none") },
            onThrow: { error in print(error.description())}
        )
```

From here it can be easily wrapped into RxSwift `Single<User?>` or Combine `AnyPublisher<User?, Error>`.

## Generated functions - Suspend, Flow and regular

The wrappers generate different return types based on the original function signature

| Original | Wrapper |
|-|-|
| `suspend` fun returning `T` | fun returning `SuspendWrapper<T>` |
| fun returning `Flow<T>` | fun returning `FlowWrapper<T>` |
| fun returning `T` | fun returning `T` |

So, for example, this class:

```kotlin
@ToNativeClass(name = LoadUserUseCaseIos)
class LoadUserUseCase(private val service: Service) {

    suspend fun loadUser(username: String) : User? = service.loadUser(username)
    
    fun observeUser(username: String) : Flow<User?> = service.observeUser(username)
    
    fun getUser(username: String) : User? = service.getUser(username)
}
```

becomes:

```kotlin
public class LoadUserUseCaseIos(private val wrapped: LoadUserUseCase) {

    public fun loadUser(username: String): SuspendWrapper<User?> =
        SuspendWrapper(null) { wrapped.loadUser(username) }

    public fun observeUser(username: String): FlowWrapper<User?> =
        FlowWrapper(null, wrapped.observeUser(username))
        
    public fun getUser(username: String): User? = wrapped.getUser(username)
    
}
```

## More options

### Customizing generated names

You can control the name of the generated class or interface:
- `@ToNativeClass(name = "MyFancyIosClass")`
- `@ToNativeInterface(name = "MyFancyIosProtocol")`

You can also omit the `name` parameter and use the defaults:
- `@ToNativeClass Foo` becomes `FooNative`
- `@ToNativeInterface Foo` becomes `FooNativeProtocol`

### Provide the scope automatically

One of the caveats of accessing suspend functions / Flows from Swift code is that you still have to provide `CoroutineScope` from the Swift code. This might upset your iOS team ;). In the spirit of keeping the shared code API as *business-focused* as possible, we can utilize `@ExportScopeProvider` to handle scopes automagically.

First you need to show the suspend wrappers where to look for the scope, like this:

```kotlin
@ExportedScopeProvider
class MainScopeProvider : ScopeProvider {

    override val scope = MainScope()
    
}
```

And then you provide the scope like this

```kotlin
@ToNativeClass(launchOnScope = MainScopeProvider::class)
```

Thanks to this, your Swift code can be simplified to just the callbacks, scope that launches coroutines is handled implicitly.

```swift
loadUserUseCaseIos.loadUser(username: "some username").subscribe(
            onSuccess: { user in print(user?.description() ?? "none") },
            onThrow: { error in print(error.description())}
        )
```

<details>
  <summary>What happens under the hood?</summary>
    
  Under the hood, a top level property `val exportedScopeProvider_mainScopeProvider = MainScopeProvider()` is created. Then, it is injected into the `SuspendWrapper`s and `FlowWrapper`s as the default scope that `launch`es the coroutines. Remember, that you can always provide custom scope if you need to.
  
  ```kotlin
  fun flow(foo: String) = FlowWrapper(exportedScopeProvider_mainScopeProvider, wrapped.flow(foo))
  fun suspending(foo: String) = SuspendWrapper(exportedScopeProvider_mainScopeProvider) { wrapped.suspending(foo) }
  ```

</details>

### Generate interfaces from classes and classes from interfaces

Usually you will just need to use `@ToNativeClass` on your business logic class like in the basic example. However, you can get more fancy, if you want. 

#### Generate interface from class

Say, you want to expose to Swift code both the class and an interface (which translates to protocol in Swift), so that you can use the protocol to create a fake impl for unit tests.

```kotlin
@ToNativeClass(name = "FooIos")
@ToNativeInterface(name = "FooIosProtocol")
class Foo
```

This code will create an interface and a class extending it.

```kotlin
interface FooIosProtocol

class FooIos(private val wrapped: Foo) : FooIosProtocol
```

#### Generate interface from interface

If you already have an interface, you can reuse it just as easily:

```kotlin
@ToNativeInterface(name = "FooIosProtocol")
interface IFoo

@ToNativeClass(name = "FooIos")
class Foo : IFoo
```

This will also create an interface and a class and automatically match them:

```kotlin
interface FooIosProtocol

class FooIos(private val wrapped: Foo) : FooIosProtocol
```

#### Generate class from interface

*Not sure what the use case might be, nevertheless, it's also possible"

```kotlin
@ToNativeClass(name = "FooIos")
interface Foo
```

Will generate:

```kotlin
class FooIos(private val wrapped: Foo)
```

## Handling from Swift side

**TBD** (Working on an article and some sample repo and some simple Combine / RxSwift wrappers that can be copied to your project).

## Download

Add the maven repository to the root `build.gradle.kts` (we will publish to jcenter later).
```kotlin
allprojects {
    repositories {
        maven(url = "https://dl.bintray.com/mklimczak/kmm-ios-suspendwrapper")
    }
}
```

In the shared module `build.gradle.kts`

```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("kapt")
    ...
}

kotlin {

  ...
  
  sourceSets {
        
        ...
  
        val commonMain by getting {
            dependencies {
                ...
                implementation("com.futuremind:ioswrapper-annotation:0.2.0")
                configurations.get("kapt").dependencies.add(
                    org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency(
                        "com.futuremind", "ioswrapper-processor", "0.2.0"
                    )
                )

            }
        }
        
        val iosMain by getting {
            ...
            kotlin.srcDir("${buildDir.absolutePath}/generated/source/kaptKotlin/")
        }
        
    }
    
}
```
