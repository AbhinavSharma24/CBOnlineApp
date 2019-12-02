package com.codingblocks.cbonlineapp.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.codingblocks.cbonlineapp.R
import com.codingblocks.cbonlineapp.util.CONVERSATION_ID
import com.codingblocks.cbonlineapp.util.extensions.getPrefs
import com.codingblocks.cbonlineapp.util.extensions.retrofitCallback
import com.codingblocks.onlineapi.Clients
import kotlinx.android.synthetic.main.fragment_inbox.*

/**
 * A simple [Fragment] subclass.
 */
class InboxFragment : Fragment() {

    private val conversationId by lazy {
        arguments?.getString(CONVERSATION_ID) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_inbox, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance(id: String) =
            InboxFragment().apply {
                arguments = Bundle().apply {
                    putString(CONVERSATION_ID, id)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView.settings.apply {
            javaScriptEnabled = true
            webView.settings.javaScriptCanOpenWindowsAutomatically = true
            webView.settings.allowFileAccess = true
            webView.settings.allowFileAccessFromFileURLs = true
        }
        if (conversationId.isNotEmpty()) {
            webView.reload()
        }
        Clients.api.getSignature().enqueue(retrofitCallback { _, response ->
            val signature = response?.body()?.get("signature")
            val userId = getPrefs()?.SP_USER_ID
            val userName = getPrefs()?.SP_USER_NAME
            val email = getPrefs()?.SP_EMAIL_ID
            val script: String
            if (conversationId.isEmpty()) {
                script = """
                    Talk.ready.then(function() {
                        var me = new Talk.User({
                            id: $userId,
                            name: "$userName",
                            email: "$email"
                        });
                        window.talkSession = new Talk.Session({
                            appId: "2LhQvB3j",
                            me: me,
                            signature: $signature
                        });
                        var inbox = talkSession.createInbox();
                        inbox.mount(document.getElementById("talkjs-container"));
                    });
                """.trimIndent()
            } else {
                script = """
                    Talk.ready.then(function() {
                        var me = new Talk.User({
                            id: $userId,
                            name: "$userName",
                            email: "$email"
                        });
                        window.talkSession = new Talk.Session({
                            appId: "2LhQvB3j",
                            me: me,
                            signature: $signature
                        });
                        var conversation = window.talkSession.getOrCreateConversation("$conversationId");
                        conversation.setParticipant(me);
                        var inbox = talkSession.createChatbox(conversation);
                        inbox.mount(document.getElementById("talkjs-container"));
                    });
                """.trimIndent()
            }

            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    super.onPageFinished(view, url)
                    view.evaluateJavascript("javascript:$script", null)
                }
            }

            webView.loadUrl("file:///android_asset/Chat.html")
        })
    }
}