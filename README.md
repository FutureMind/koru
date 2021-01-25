# SuspendWrapper

Automatically generates wrappers for `suspend` functions and `Flow` for easy access from Swift code in Kotlin Multiplatform projects.

Inspired by https://touchlab.co/kotlin-coroutines-rxswift/ by Russell Wolf.

**Note**: this is an **early preview**. Both the library api and maven coordinates are very likely to change.

## Basic sample

Let's say you have a class in the `shared` module, that looks like this:

```kotlin
@WrapForIos
class LoadUserUseCase(private val service: Service) {

    suspend fun loadUser(username: String) : User? = service.loadUser(username)
    
}
```

Such use case can be easily consumed from Android code, but in Kotlin Native (iOS) suspend functions generate a completion handler which is a bit of a PITA to work with.

When you add `@WrapForIos` annotation to the class, a wrapper is generated:

```kotlin
public class LoadUserUseCaseIos(private val wrapped: LoadUserUseCase) {

  public fun loadUser(username: String): SuspendWrapper<User?> = 
      SuspendWrapper(null) { wrapped.loadUser(username) }
  
}
```

Notice that in place of `suspend` function, we get a function exposing `SuspendWrapper`. When you expose `LoadUserUseCaseIos` to your Swift code, it can be consumed like this:

```swift
loadUserUseCaseIos.loadUser(username: "some username").subscribe(
            scope: coroutineScope, //this can be provided automatically, more on that below
            onSuccess: { user in print(user?.description() ?? "none") },
            onThrow: { error in print(error.description())}
        )
```

From here it can be easily wrapped into RxSwift `Single<User?>` or Combine `AnyPublisher<User?, Error>`.

## More options

### Suspend, Flow and blocking function

The wrappers handle original function signatures in three ways:

| Original | Wrapper |
|-|-|
| `suspend` fun returning `T` | fun returning `SuspendWrapper<T>` |
| fun returning `Flow<T>` | fun returning `FlowWrapper<T>` |
| fun returning `T` | fun returning `T` |

So, for example, this class:

```kotlin
@WrapForIos
class LoadUserUseCase(private val service: Service) {

    suspend fun loadUser(username: String) : User? = service.loadUser(username)
    
    fun observeUser(username: String) : Flow<User?> = service.observeUser(username)
    
    fun getUser(username: String) : User? = service.getUser(username)
}
```

becomes:

```kotlin
public class LoadUserUseCaseIos(private val wrapped: LoadUserUseCase) {

    public fun getUser(username: String): User? = wrapped.getUser(username)

    public fun loadUser(username: String): SuspendWrapper<User?> =
        SuspendWrapper(null) { wrapped.loadUser(username) }

    public fun observeUser(username: String): FlowWrapper<User?> =
        FlowWrapper(null, wrapped.observeUser(username))
}
```

### Generate interfaces

If you write tests in your Swift code, you probably need protocols for your classes to use them as fakes in tests. In KMM protocols are derived from kotlin interfaces. Generated wrapper classes need their own interfaces and you've got two options to create them.

#### Generate interface from class

The easiest way is to just generate them automagically.

```kotlin
@WrapForIos(generateInterface = true)
class LoadUserUseCase(private val service: Service) {

    suspend fun loadUser(username: String) = service.loadUser(username)
    
}
```

This will create both the wrapper class `LoadUserUseCaseIos` and a corresponding interface `LoadUserUseCaseIosProtocol`.

#### Generate interface from interface

Or if you already have an interface, you can also `@WrapForIos` so that appropriate signatures are created for iOS protocols.

```kotlin
@WrapForIos
interface LoadUserUseCase {

    suspend fun loadUser(username: String): User?
    
}

@WrapForIos
class LoadUserUseCaseImpl(private val service: Service) : LoadUserUseCase {

    override suspend fun loadUser(username: String) = service.loadUser(username)
    
}
```

This will create the wrapper class `LoadUserUseCaseImplIos` and a corresponding interface `LoadUserUseCaseIos`.

**Note**: the annotation for the interface will change in future versions.

### Customizing generated names

*`To be documented`, but generally you can use `@WrapForIos(className = "MyOwnIosClassName")` and `@WrapForIos(generatedInterfaceName = "MyOwnIosProtocolName")`*

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
@WrapForIos(launchOnScope = MainScopeProvider::class)
```

Under the hood it generates a top level `MainScopeProvider` property which is then injected into the `SuspendWrapper`s and `FlowWrapper`s. Thanks to this, your iOS code can be simplified to just the callbacks, scope that launches coroutines is handled implicitly:

```swift
loadUserUseCaseIos.loadUser(username: "some username").subscribe(
            onSuccess: { user in print(user?.description() ?? "none") },
            onThrow: { error in print(error.description())}
        )
```


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
                implementation("com.futuremind:ioswrapper-annotation:0.1.1")
                configurations.get("kapt").dependencies.add(
                    org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency(
                        "com.futuremind", "ioswrapper-processor", "0.1.1"
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
