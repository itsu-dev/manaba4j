package dev.itsu.manaba

import dev.itsu.manaba.model.Assignment
import dev.itsu.manaba.model.Course
import org.jsoup.Jsoup
import java.text.SimpleDateFormat

object Manaba {

    private const val URL_LOGIN_1 = "https://idp.account.tsukuba.ac.jp/idp/profile/SAML2/Redirect/SSO?execution=e1s1"
    private const val URL_LOGIN_2 = "https://idp.account.tsukuba.ac.jp/idp/profile/SAML2/Redirect/SSO?execution=e1s2"
    private const val URL_HOST = "https://manaba.tsukuba.ac.jp"
    private const val URL_SAML2 = "$URL_HOST/Shibboleth.sso/SAML2/POST"
    const val URL_HOME = "$URL_HOST/ct/home"
    const val URL_ASSIGNMENTS = "$URL_HOST/ct/home_library_query"
    const val URL_COURSE = "$URL_HOST/ct/home_course"

    private var loggedIn = false

    fun login(userName: String, password: String): Boolean {
        if (loggedIn) return true

        Http.get(Http.getRedirectURL(URL_HOME))

        Http.post(
            URL_LOGIN_1,
            mapOf(
                "shib_idp_ls_exception.shib_idp_session_ss" to "",
                "shib_idp_ls_success.shib_idp_session_ss" to "false",
                "shib_idp_ls_value.shib_idp_session_ss" to "",
                "shib_idp_ls_exception.shib_idp_persistent_ss" to "",
                "shib_idp_ls_success.shib_idp_persistent_ss" to "false",
                "shib_idp_ls_value.shib_idp_persistent_ss" to "",
                "shib_idp_ls_supported" to "",
                "_eventId_proceed" to ""
            )
        )

        val posted = Http.post(
            URL_LOGIN_2,
            mapOf(
                "_eventId_proceed" to "",
                "j_username" to userName,
                "j_password" to password
            )
        )

        val parsed = Jsoup.parse(posted)
        val relayState = parsed.getElementsByAttributeValueContaining("name", "RelayState").first()?.`val`()
        val samlResponse = parsed.getElementsByAttributeValueContaining("name", "SAMLResponse").first()?.`val`()

        if (relayState == null || samlResponse == null) return false

        Http.post(
            URL_SAML2,
            mapOf(
                "RelayState" to relayState,
                "SAMLResponse" to samlResponse,
            )
        )

        loggedIn = true

        return true
    }

    fun getAssignments(): List<Assignment> {
        if (!loggedIn) return emptyList()

        val document = Jsoup.parse(Http.get(URL_ASSIGNMENTS))
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm")
        val result = mutableListOf<Assignment>()
        document.getElementsByClass("stdlist")
            .first()!!
            .allElements
            .filter { it.classNames().contains("row0") || it.classNames().contains("row1") }
            .forEach { tr ->
                var title = ""
                var url = ""
                var type = ""
                var typeURL = ""
                var course = ""
                var courseURL = ""
                var beginAt = 0L
                var expiredAt = 0L

                tr.getElementsByTag("td").forEachIndexed { index, element ->
                    when (index) {
                        0 -> {
                            val a = element.getElementsByTag("a").first()!!
                            type = a.text()
                            typeURL = "$URL_HOST/ct/${a.attr("href")}"
                        }
                        1 -> {
                            val a = element.getElementsByTag("a").first()!!
                            title = a.text()
                            url = "$URL_HOST/ct/${a.attr("href")}"
                        }
                        2 -> {
                            val a = element.getElementsByTag("a").first()!!
                            course = a.text()
                            courseURL = "$URL_HOST/ct/${a.attr("href")}"
                        }
                        3 -> {
                            beginAt = if (element.text().isEmpty()) 0L else format.parse(element.text()).time
                        }
                        4 -> {
                            expiredAt = if (element.text().isEmpty()) 0L else format.parse(element.text()).time
                        }
                    }
                }

                result.add(Assignment(title, url, type, typeURL, course, courseURL, beginAt, expiredAt))
            }
        return result.toList()
    }

    fun getCourses(): List<Course> {
        if (!loggedIn) return emptyList()

        val document = Jsoup.parse(Http.get(URL_COURSE))
        val result = mutableListOf<Course>()
        document.getElementsByClass("courselist")
            .first()!!
            .allElements
            .filter {
                (it.classNames().contains("row0") || it.classNames().contains("row1")) && it.classNames()
                    .contains("courselist-c")
            }
            .forEach { tr ->
                var title = ""
                var url = ""
                var isNewsAvailable = false
                var isTestOrQuestionAvailable = false
                var isReportAvailable = false
                var isThreadAvailable = false
                var isIndividualAvailable = false
                var year = 0
                var information = ""
                var teachers = listOf<String>()

                tr.getElementsByTag("td").forEachIndexed { index, element ->
                    when (index) {
                        0 -> {
                            element.getElementsByClass("courselist-title").first()!!
                                .getElementsByTag("a").first()!!
                                .let {
                                    title = it.text()
                                    url = "$URL_HOST/ct/${it.attr("href")}"
                                }

                            element.getElementsByClass("course-card-status").first()!!
                                .getElementsByTag("img")
                                .forEachIndexed { i, img ->
                                    when (i) {
                                        0 -> isNewsAvailable = img.attr("src") == "/icon_coursenews_on.png"
                                        1 -> isTestOrQuestionAvailable = img.attr("src") == "/icon-coursedeadline-on.png"
                                        2 -> isReportAvailable = img.attr("src") == "/icon-coursegrad-on.png"
                                        3 -> isThreadAvailable = img.attr("src") == "/icon_coursethread_on.png"
                                        4 -> isIndividualAvailable = img.attr("src") == "/icon_collist_individual-on.png"
                                    }
                                }
                        }

                        1 -> {
                            year = element.text().toInt()
                        }

                        2 -> {
                            information = element.getElementsByTag("span").first()!!.text()
                        }

                        3 -> {
                            teachers = element.text().split(",").toList()
                        }
                    }
                }

                result.add(
                    Course(
                        title,
                        url,
                        isNewsAvailable,
                        isTestOrQuestionAvailable,
                        isReportAvailable,
                        isThreadAvailable,
                        isIndividualAvailable,
                        year,
                        information,
                        teachers
                    )
                )
            }
        return result.toList()
    }
}