package com.example.ebird.navigation

sealed class NavigationItem(val route: String) {
    data object GoogleMap : NavigationItem(Screens.GoogleMapScreen.name)
    data object BirdsScreen : NavigationItem(Screens.Birds.name)
    data object SignUpScreen : NavigationItem(Screens.SignUpScreen.name)
    data object SignInScreen : NavigationItem(Screens.SignInScreen.name)
    data object SettingsScreen:NavigationItem(Screens.Settings.name)
    data object BirdsGalleryScreen:NavigationItem(Screens.BirdsGallery.name)
    data object FavBirds:NavigationItem(Screens.FavBirds.name)
}