package com.example.mindlog.selfaware

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class SelfAwareDispatcher(private val cls: Class<*>) : Dispatcher() {
    private fun fx(p: String) = requireNotNull(cls.classLoader).getResource(p)!!.readText()

    override fun dispatch(req: RecordedRequest): MockResponse = when {
        req.path!!.startsWith("/selfaware/daily") && req.method == "GET" ->
            MockResponse().setResponseCode(200).setBody(fx("api/today_success.json"))
        req.path!!.startsWith("/selfaware/answers") && req.method == "POST" ->
            MockResponse().setResponseCode(200).setBody(fx("api/answer_create_success.json"))
        req.path!!.startsWith("/selfaware/history") && req.requestUrl?.queryParameter("page") == "1" ->
            MockResponse().setResponseCode(200).setBody(fx("api/history_page1.json"))
        req.path!!.startsWith("/selfaware/history") && req.requestUrl?.queryParameter("page") == "2" ->
            MockResponse().setResponseCode(200).setBody(fx("api/history_page2.json"))
        else -> MockResponse().setResponseCode(404)
    }
}