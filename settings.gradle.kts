pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // Все, что не нашлось в google(), будет искаться здесь.
        // Это и есть правильное место для org.xbill:dns
        mavenCentral()
    }
}

rootProject.name = "DnsRewriter"
include(":app")