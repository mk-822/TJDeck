package net.totoraj.tjdeck.exception

import twitter4j.TwitterException

class AccessTokenException(cause: Exception) : TwitterException(cause)