# Change Log

## [0.8.0] - 2021-09-06
### Fixed
- The generated classes now only extend annotated interfaces. Methods and vals of non-annotated interfaces are still wrapped, but not inherited from.  
- Methods and vals will not add override modifier just because an unrelated interface had a method / val with identical name (fixed matching false positives).  

## [0.7.0] - 2021-07-29
### Changed
- Make the annotation processor incremental

## [0.6.0] - 2021-07-01
### Added
- `@ToNativeClass(freeze = true)` makes the wrapper classes and their jobs frozen.
- keep `internal` visibility and explicitly throw on `private` for wrapped classes (#30 by [JohNan](https://github.com/JohNan))

## [0.5.0] - 2021-06-06
### Changed
- Inject ScopeProvider as a constructor parameter for more flexibility (#22 by [Takahiro Menju](https://github.com/takahirom))
### Fixed
- Compatibility with Kotlin compiler plugin 1.5.x
