package net.totoraj.tjdeck.model.exception

import twitter4j.TwitterException

class RequestTokenException(cause: Exception) : TwitterException(cause) {
}