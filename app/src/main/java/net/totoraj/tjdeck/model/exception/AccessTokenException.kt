package net.totoraj.tjdeck.model.exception

import twitter4j.TwitterException

class AccessTokenException(cause: Exception) : TwitterException(cause)