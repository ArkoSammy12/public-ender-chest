package io.github.arkosammy12.publicenderchest.logging

enum class TimeQueryType(val commandNodeName: String) {
    BEFORE("before"),
    AFTER("after");

    companion object {

        fun getFromCommandNodeName(name: String) : TimeQueryType? {
            for (timeQueryType: TimeQueryType in entries) {
                if (name == timeQueryType.commandNodeName) {
                    return timeQueryType
                }
            }
            return null
        }

    }

}