[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.futuremind/koru/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.futuremind/koru)

# Koru

Automatically generates wrappers for `suspend` functions and `Flow` for easy access from Swift code in Kotlin Multiplatform projects.

Inspired by https://touchlab.co/kotlin-coroutines-rxswift/ by Russell Wolf.

**Note**: version 0.11.0 introduces [KSP](https://kotlinlang.org/docs/ksp-overview.html) support. Support for kapt will still be provided for some time.

## Getting started

To get started, consult the Basic example below, read [introductory article](https://medium.com/futuremind/handling-kotlin-multiplatform-coroutines-in-swift-koru-4a80b93f232b) or check out the [example repo](https://github.com/FutureMind/koru-example).

### Basic example

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

## Generated functions / properties - Suspend, Flow and regular

The wrappers generate different return types based on the original member signature

| Original | Wrapper |
|-|-|
| `suspend` fun returning `T` | fun returning `SuspendWrapper<T>` |
| fun returning `Flow<T>` | fun returning `FlowWrapper<T>` |
| fun returning `T` | fun returning `T` |
| val / var returning `Flow<T>` | val returning `FlowWrapper<T>` |
| val / var returning `T` | val returning `T` |

So, for example, this class:

```kotlin
@ToNativeClass(name = "LoadUserUseCaseIos")
class LoadUserUseCase(private val service: Service) {

    suspend fun loadUser(username: String) : User? = service.loadUser(username)
    
    fun observeUser(username: String) : Flow<User?> = service.observeUser(username)
    
    fun getUser(username: String) : User? = service.getUser(username)

    val someone : User? get() = service.getUser("someone")

    val someoneFlow : Flow<User> = service.observeUser("someone")

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

    public val someone: User?
        get() =  wrapped.someone

    public val someoneFlow: FlowWrapper<User>
        get() = com.futuremind.koru.FlowWrapper(null, wrapped.someoneFlow)
    
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
    
  Under the hood, a top level property `val exportedScopeProvider_mainScopeProvider = MainScopeProvider()` is created. Then, it is injected into the constructor of the wrapped class and then into `SuspendWrapper`s and `FlowWrapper`s as the default scope that `launch`es the coroutines. Remember, that you can always override with your custom scope if you need to.
  
  ```kotlin
    public class LoadUserUseCaseIos(
      private val wrapped: LoadUserUseCase,
      private val scopeProvider: ScopeProvider?
    ) {
      fun flow(foo: String) = FlowWrapper(scopeProvider, wrapped.flow(foo))
      fun suspending(foo: String) = SuspendWrapper(scopeProvider) { wrapped.suspending(foo) }
    }
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

*Not sure what the use case might be, nevertheless, it's also possible:

```kotlin
@ToNativeClass(name = "FooIos")
interface Foo
```

Will generate:

```kotlin
class FooIos(private val wrapped: Foo)
```

## Handling in Swift code

You can consume the coroutine wrappers directly as callbacks. But if you are working with Swift Combine, you can wrap those callbacks using [simple global functions](https://github.com/FutureMind/koru-example/blob/master/iosApp/iosApp/Utils/Coroutine2Combine.swift) (extension functions are not supported for Kotlin Native generic types at this time).

Then, you can call them like this:

```swift
createPublisher(wrapper: loadUserUseCase.loadUser(username: "Bob"))
    .sink(
        receiveCompletion: { completion in print("Completion: \(completion)") },
        receiveValue: { user in print("Hello from the Kotlin side \(user?.name)") }
    )
    .store(in: &cancellables)
```

Similar helper functions can be easily created for RxSwift.

## Download

The artifacts are available on Maven Central and the compiler plugin in Gradle Plugin Portal. 

To use the library in a KMM project, use this config in the `build.gradle.kts`:

```kotlin
plugins {
    //add ksp and koru compiler plugin
    id("com.google.devtools.ksp") version "1.6.21-1.0.6"
    id("com.futuremind.koru").version("0.11.1")
}

kotlin {
  
  sourceSets {
  
        val commonMain by getting {
            dependencies {
                // add library dependency
                implementation("com.futuremind:koru:0.11.1")
            }
        }
      
        val iosMain by creating {
            ...
        }
        
    }
    
}

koru {
    // let the compiler plugin know where the generated code should be available
    // by providing the name of ios source set
    nativeSourceSetNames = listOf("iosMain")
}
```

<details>
  <summary>Legacy kapt support</summary>

Starting from version 0.11.0 this library supports `ksp` which is the recommended way. `kapt` is
still available, though, with the following configuration.

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
                implementation("com.futuremind:koru:0.12.0")
                configurations.get("kapt").dependencies.add(
                    org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency(
                        "com.futuremind", "koru-processor", "0.12.0"
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

</details>

### Compatibility

| Koru             | KSP                                         | Kotlin                    |
|------------------|---------------------------------------------|---------------------------|
| 0.11.1<br>0.12.0 | 1.6.21-1.0.6<br>1.7.0-1.0.6<br>1.7.10-1.0.6 | 1.6.21<br>1.7.0<br>1.7.10 |

This library should be compatible with any version of coroutines.

If you find any compatibility issues, let us know.
