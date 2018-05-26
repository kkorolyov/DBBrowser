# Change Log

## 4.0 - 2018-05-24
### Changes
* Overhauled architecture _again_
	* Basic usage involves using a `Session` to execute various `Request` implementations

## [3.0] - 2017-05-11
### Changes
* Overhauled + modularized architecture
* Added `Mapper` for finer Java-SQL mapping customization
* Added `Converter`
### Fixes
* Made the thing more **robust**

## [2.1.1] - 2017-03-05
### Fixes
* Fixed regression of SqlobFields overwriting

## [2.1] - 2017-02-26
### Changes
* `Session.flush()` no longer closes the underlying connection
* Compound Conditions support custom grouping
* Added empty Condition constructor
### Fixes
* `transient` and `static` fields are ignored by the persistence engine

## [2.0] - 2016-12-13
### Changes
* Overhauled from scratch to be a Java object persistence library

## [1.0] - 2016-08-14
* Initial release

[3.0]: https://github.com/kkorolyov/SQLOb/releases/tag/3.0
[2.1]: https://github.com/kkorolyov/SQLOb/releases/tag/v2.1
[2.0]: https://github.com/kkorolyov/SQLOb/releases/tag/v2.0
[1.0]: https://github.com/kkorolyov/SQLOb/releases/tag/1.0
