README

If any external libraries could not be found in Gradle repos (such as JCenter),
we may add the source codes here.

Then do not forget to update settings.gradle and app/build.gradle as well:

-- settings.gradle --

Include the library sources just added, e.g:

include ':app', ':libraries:Donations'

-- app/build.gradle

dependencies {
...
compile project(':libraries:Donations')
...
}
