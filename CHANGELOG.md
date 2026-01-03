# Changelog

## 1.0.1 (TBA)

* Fix: We did not properly run our map cleanup logic on disable previously. Also, we fully remove our shopkeeper marker sets from all maps now during disable.
* Fix: When removing a shopkeeper marker, we no longer use the shopkeeper's world, since it might have changed in the meantime. Instead, we check all maps for the marker corresponding to the shopkeeper.
* Improve the handling of BlueMap API lifecycle changes: Use a fair ReentrantLock for all of our BlueMap operations to ensure a consistent ordering of map operations and block during disable while there are still other map operations active on the server main thread.
* Write web assets asynchronously.
* Add project URL to plugin.yml.
* Add api-version to plugin.yml.

## 1.0.0 (2025-01-03)

* Initial version.
