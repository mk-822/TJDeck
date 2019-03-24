package net.totoraj.tjdeck.exception

import twitter4j.TwitterException

class RequestTokenException(cause: Exception) : TwitterException(cause) {
}