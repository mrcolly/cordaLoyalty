package com.snam.POJO

import java.util.*

data class ResponsePojo(
        val outcome : String = "",
        val message : String = "",
        val data : Any? = null
)