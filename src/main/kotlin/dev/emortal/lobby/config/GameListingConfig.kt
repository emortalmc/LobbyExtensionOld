package dev.emortal.lobby.config

@kotlinx.serialization.Serializable
class GameListingConfig(
    var gameListings: HashMap<String, GameListing> = hashMapOf()
)