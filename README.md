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
  public fun loadUser(username: String): SuspendWrapper<User?> = SuspendWrapper(null) { wrapped.loadUser(username) }
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

*`To be documented` but generally suspend becomes SuspendWrapper, Flow becomes FlowWrapper and regular functions are called directly.*

### Generate interfaces

*`To be documented` but there are two options. Either use `@WrapForIos(generateInterface = true)` or add `@WrapForIos` to both the class and the interface it extends. The purpose of this is to provide protocols for faking in Swift tests.*

### Customizing generated names

*`To be documented`*

### Provide the scope automatically

*`To be documented`* but generally:

```
@ExportedScopeProvider
class MainScopeProvider : ScopeProvider {
    override val scope = MainScope()
}
```

...and then `@WrapForIos(launchOnScope = MainScopeProvider::class)`


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

kapt {
    correctErrorTypes = true
}

```
