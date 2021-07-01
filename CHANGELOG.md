# Change Log

## [0.6.0] - 2021-07-01
### Added
- `@ToNativeClass(freeze = true)` makes the wrapper classes and their jobs frozen.
- keep `internal` visibility and explicitly throw on `private` for wrapped classes (#30 by [JohNan](https://github.com/JohNan))


## [0.5.0] - 2021-06-06
### Changed
- Inject ScopeProvider as a constructor parameter for more flexibility (#22 by [Takahiro Menju](https://github.com/takahirom))
### Fixed
- Compatibility with Kotlin compiler plugin 1.5.x
