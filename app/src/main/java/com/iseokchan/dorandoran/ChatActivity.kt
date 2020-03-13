package com.iseokchan.dorandoran

import android.R.attr
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.iseokchan.dorandoran.adapters.ChatAdapter
import com.iseokchan.dorandoran.models.Chat
import com.iseokchan.dorandoran.models.ChatRoom
import kotlinx.android.synthetic.main.activity_chat.*


class ChatActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var rootRef: FirebaseFirestore
    private lateinit var currentUser: FirebaseUser

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: ChatAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    private var chatroom_id = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        if (!intent.hasExtra("uid") || !intent.hasExtra("chatroom_id")) {

            // TODO : 전달된 값 없을 경우?
            return
        }

        this.chatroom_id = intent.getStringExtra("chatroom_id")

        auth = FirebaseAuth.getInstance()
        rootRef = FirebaseFirestore.getInstance()

        viewManager = LinearLayoutManager(this)
        viewAdapter = ChatAdapter(ArrayList(), intent.getStringExtra("uid"))

        recyclerView = rv_chats.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter

        }

        recyclerView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (attr.bottom < oldBottom) {
                recyclerView.postDelayed(
                    Runnable { recyclerView.smoothScrollToPosition(this.viewAdapter.itemCount-1) },
                    100
                )
            }
        }

        btn_sendMessage.setOnClickListener {

            val message = et_message.text.toString()
            val chatRoomRef = rootRef.collection("chatrooms").document(chatroom_id)
            chatRoomRef.update("messages", FieldValue.arrayUnion(Chat(
                message,
                currentUser.uid,
                Timestamp.now().toDate()
            )))
            et_message.setText("")

        }
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        auth.currentUser?.let {
            this.currentUser = it
        }

        updateUI(currentUser)
    }

    fun updateUI(currentUser: FirebaseUser?) {

        if (currentUser !== null) {

            getChattings()

        } else {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    private fun getChattings() {

        val chatRoomRef = rootRef.collection("chatrooms").document(chatroom_id)

        chatRoomRef.addSnapshotListener { value, e ->

            if (e != null) {
                Log.w("MyTag", "Listen failed.", e)
                return@addSnapshotListener
            }
            val document = value!!

            val chatRoom = document.toObject(ChatRoom::class.java)

            updateChatView(chatRoom!!)

        }
    }

    private fun updateChatView(chatRoom: ChatRoom) {

        chatRoom.messages?.let {

            this.viewAdapter.updateList(it)
            this.recyclerView.smoothScrollToPosition(this.viewAdapter.itemCount-1)

        }

    }
}