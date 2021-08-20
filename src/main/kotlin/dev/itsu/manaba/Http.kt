package dev.itsu.manaba

import java.net.*
import java.nio.charset.StandardCharsets

object Http {

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36"
    private const val ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
    private const val ACCEPT_LANGUAGE = "ja-JP,ja;q=0.9,en-US;q=0.8,en;q=0.7,zh-CN;q=0.6,zh;q=0.5"
    private const val CONTENT_TYPE = "application/x-www-form-urlencoded; charset=utf-8"

    private val cookieManager = CookieManager().also {
        it.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        CookieHandler.setDefault(it)
    }

    fun post(url: String, data: Map<String, Any>, properties: Map<String, String> = mapOf()): String =
        (URL(url).openConnection() as HttpURLConnection).let {
            it.doOutput = true
            it.instanceFollowRedirects = false
            it.requestMethod = "POST"
            it.setRequestProperty("User-Agent", USER_AGENT)
            it.setRequestProperty("Accept", ACCEPT)
            it.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE)
            it.setRequestProperty("Content-Type", CONTENT_TYPE)

            properties.forEach { prop ->
                it.setRequestProperty(prop.key, prop.value)
            }

            it.outputStream.bufferedWriter(StandardCharsets.UTF_8).use {
                var postData = ""
                data.forEach {
                    postData += "&${URLEncoder.encode(it.key, StandardCharsets.UTF_8.toString())}=${URLEncoder.encode(it.value.toString(), StandardCharsets.UTF_8.toString())}"
                }
                it.write(postData)
            }
            it.inputStream.bufferedReader(StandardCharsets.UTF_8).use {
                return@use it.readText()
            }
        }

    fun get(url: String, properties: Map<String, String> = mapOf()): String =
        (URL(url).openConnection() as HttpURLConnection).let {
            it.doOutput = false
            it.instanceFollowRedirects = false
            it.requestMethod = "GET"
            it.setRequestProperty("User-Agent", USER_AGENT)
            it.setRequestProperty("Accept", ACCEPT)
            it.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE)
            it.setRequestProperty("Content-Type", CONTENT_TYPE)

            properties.forEach { prop ->
                it.setRequestProperty(prop.key, prop.value)
            }
            it.inputStream.bufferedReader(StandardCharsets.UTF_8).use {
                return@use it.readText()
            }
        }


    fun getRedirectURL(url: String): String =
        (URL(url).openConnection() as HttpURLConnection).let {
            it.doOutput = false
            it.instanceFollowRedirects = false
            it.requestMethod = "GET"
            it.setRequestProperty("User-Agent", USER_AGENT)
            it.setRequestProperty("Accept", ACCEPT)
            it.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE)
            it.setRequestProperty("Content-Type", CONTENT_TYPE)
            it.headerFields.get("Location")?.firstOrNull() ?: ""
        }

}