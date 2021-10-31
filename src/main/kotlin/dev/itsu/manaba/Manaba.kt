package dev.itsu.manaba

import dev.itsu.manaba.model.*
import org.jsoup.Jsoup
import java.text.SimpleDateFormat

object Manaba {

    private const val URL_LOGIN_1 = "https://idp.account.tsukuba.ac.jp/idp/profile/SAML2/Redirect/SSO?execution=e1s1"
    private const val URL_LOGIN_2 = "https://idp.account.tsukuba.ac.jp/idp/profile/SAML2/Redirect/SSO?execution=e1s2"
    private const val URL_HOST = "https://manaba.tsukuba.ac.jp"
    private const val URL_SAML2 = "$URL_HOST/Shibboleth.sso/SAML2/POST"
    const val URL_HOME = "$URL_HOST/ct/home"
    const val URL_ASSIGNMENTS = "$URL_HOST/ct/home_library_query"
    const val URL_CURRENT_COURSES = "$URL_HOST/ct/home_course?chglistformat=list"
    const val URL_ALL_COURSES = "$URL_HOST/ct/home_course_all?chglistformat=list"
    const val URL_PAST_COURSES = "$URL_HOST/ct/home_course_past?chglistformat=list"
    const val URL_UPCOMING_COURSES = "$URL_HOST/ct/home_course_upcoming?chglistformat=list"
    const val URL_COURSE = "$URL_HOST/ct/course_%s" // courseId
    const val URL_COURSE_NEWSES = "$URL_HOST/ct/course_%s_news" // courseId
    const val URL_COURSE_CONTENTS = "$URL_HOST/ct/course_%s_page"
    const val URL_COURSE_THREADS = "$URL_HOST/ct/course_%s_topics" // courseId

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

    fun getCurrentCourses(): List<Course> = getCourses(URL_CURRENT_COURSES)

    fun getAllCourses(): List<Course> = getCourses(URL_ALL_COURSES)

    fun getPastCourses(): List<Course> = getCourses(URL_PAST_COURSES)

    fun getUpcomingCourses(): List<Course> = getCourses(URL_UPCOMING_COURSES)

    private fun getCourses(url: String): List<Course> {
        if (!loggedIn) return emptyList()

        val document = Jsoup.parse(Http.get(url))
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
                var imageUrl = ""
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
                            element.getElementsByTag("img").first()!!
                                .let {
                                    imageUrl = it.attr("src")
                                }
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
                                        1 -> isTestOrQuestionAvailable =
                                            img.attr("src") == "/icon-coursedeadline-on.png"
                                        2 -> isReportAvailable = img.attr("src") == "/icon-coursegrad-on.png"
                                        3 -> isThreadAvailable = img.attr("src") == "/icon_coursethread_on.png"
                                        4 -> isIndividualAvailable =
                                            img.attr("src") == "/icon_collist_individual-on.png"
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
                        imageUrl,
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

    fun getContents(courseId: String): List<ContentTitle> {
        if (!loggedIn) return emptyList()

        val document = Jsoup.parse(Http.get(URL_COURSE_CONTENTS.format(courseId)))
        val result = mutableListOf<ContentTitle>()
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm")

        document.getElementsByClass("contentslist")
            .first()!!
            .getElementsByTag("tbody")
            .first()!!
            .getElementsByTag("tr")
            .forEach { tr ->
                var title = ""
                var url = ""
                var pageCount = 0
                var updatedAt = 0L

                tr.getElementsByClass("about-contents").first()!!
                    .getElementsByTag("a").first()!!
                    .let { a ->
                        title = a.text()
                        url = "$URL_HOST/ct/${a.attr("href")}"
                    }

                tr.getElementsByClass("info-contents").first()!!
                    .getElementsByTag("div").first()!!
                    .allElements.first()!!
                    .text().split(" ページ")
                    .let {
                        pageCount = it[0].replace("全 ", "").toInt()
                        updatedAt = format.parse(it[1]).time
                    }

                result.add(
                    ContentTitle(
                        title,
                        url,
                        pageCount,
                        updatedAt
                    )
                )
            }
        return result.toList()
    }

    fun getCourseNewses(courseId: String): List<NewsTitle> {
        if (!loggedIn) return emptyList()

        val document = Jsoup.parse(Http.get(URL_COURSE_NEWSES.format(courseId)))
        val result = mutableListOf<NewsTitle>()
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm")

        document.getElementsByClass("stdlist")
            .first()!!
            .getElementsByTag("tbody")
            .first()!!
            .getElementsByTag("tr")
            .forEachIndexed { index, tr ->
                if (index == 0) return@forEachIndexed

                var title = ""
                var newsUrl = ""
                var author = ""
                var updatedAt = 0L

                tr.getElementsByTag("td")[0].let { td ->
                    td.getElementsByTag("a").let { a ->
                        title = a.text()
                        newsUrl = "$URL_HOST/ct/${a.attr("href")}"
                    }
                }

                tr.getElementsByTag("td")[1].let { td ->
                    author = td.getElementsByTag("a")[1].text()
                }

                tr.getElementsByTag("td")[2].let { td ->
                    updatedAt = format.parse(td.text().trimStart().trimEnd()).time
                }

                result.add(
                    NewsTitle(
                        title,
                        newsUrl,
                        author,
                        updatedAt
                    )
                )
            }
        return result.toList()
    }

    fun getNews(newsUrl: String): News? {
        if (!loggedIn) return null

        val document = Jsoup.parse(Http.get(newsUrl))
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm")

        return News(
            document.getElementsByClass("msg-subject").first()!!.text(),
            document.getElementsByClass("msg-info").first()!!.getElementsByTag("a")[1].text(),
            document.getElementsByClass("msg-text").first()!!.html(),
            format.parse(document.getElementsByClass("msg-date").first()!!.text().trimEnd()).time
        )
    }

    fun getContent(contentUrl: String): Content? {
        if (!loggedIn) return null

        val document = Jsoup.parse(Http.get(contentUrl))
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm")

        val limit = document.getElementsByClass("pagelimitview").first()!!
            .textNodes().first()!!
            .text()
            .replace("公開期間: ", "").split(" ～ ")

        val pages = mutableMapOf<String, String>()
        document.getElementsByClass("contentslist").first()!!
            .getElementsByTag("li").forEach { li ->
                li.getElementsByTag("a").first()!!.let { a ->
                    pages[a.text()] = "$URL_HOST/ct/${a.attr("href")}"
                }
            }

        return Content(
            document.getElementsByClass("contents").first()!!.getElementsByTag("a").first()!!.text(),
            contentUrl,
            document.getElementsByClass("pagetitle").first()!!.text(),
            document.getElementsByClass("articletext").first()!!.html(),
            format.parse(document.getElementsByClass("contents-modtime").first()!!.text().replace("更新日時 : ", "")).time,
            format.parse(limit[0]).time,
            if (limit[1].isEmpty()) 0L else format.parse(limit[1]).time,
            pages.toMap()
        )
    }

    /**
     * @param courseUrl Course URL
     */
    fun getCourseIdFromURL(courseUrl: String): String {
        return courseUrl.split("/").last().split("_")[0]
    }

}