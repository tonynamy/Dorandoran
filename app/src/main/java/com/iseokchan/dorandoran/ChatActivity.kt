package com.iseokchan.dorandoran

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.iseokchan.dorandoran.adapters.ChatAdapter
import com.iseokchan.dorandoran.models.Chat
import com.iseokchan.dorandoran.models.ChatRoom
import com.iseokchan.dorandoran.models.User
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class ChatActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var rootRef: FirebaseFirestore
    private lateinit var currentUser: FirebaseUser

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: ChatAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    private lateinit var actionBar: ActionBar

    private var chatRoomListener: ListenerRegistration? = null

    private var chatroom_id = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        auth = FirebaseAuth.getInstance()

        if (!intent.hasExtra("chatroom_id") || auth.currentUser == null) {

            return
        }

        this.currentUser = auth.currentUser!!
        this.chatroom_id = intent.getStringExtra("chatroom_id")

        rootRef = FirebaseFirestore.getInstance()

        viewManager = LinearLayoutManager(this)
        viewAdapter = ChatAdapter(ChatRoom(), this.currentUser.uid)

        recyclerView = rv_chats.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter

        }

        recyclerView.addOnLayoutChangeListener { _, _, _, bottom, _, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                recyclerView.postDelayed(
                    Runnable { recyclerView.smoothScrollToPosition(this.viewAdapter.itemCount.minus(1)) },
                    100
                )
            }
        }

        btn_sendMessage.setOnClickListener {

            val message = et_message.text.toString()
            val chatRoomRef = rootRef.collection("chatrooms").document(chatroom_id)
            chatRoomRef.update(
                "messages", FieldValue.arrayUnion(
                    Chat(
                        message,
                        currentUser.uid,
                        Timestamp.now().toDate()
                    )
                )
            )
            et_message.setText("")

        }

        this.actionBar = supportActionBar!!
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        auth.currentUser?.let {
            this.currentUser = it
        }

        updateUI(currentUser)
    }

    private fun updateUI(currentUser: FirebaseUser?) {

        if (currentUser !== null) {

            addSnapshotListener()

        } else {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        this.chatRoomListener?.remove()
    }

    private fun addSnapshotListener() {

        if(this.chatRoomListener == null ){
            val chatRoomRef = rootRef.collection("chatrooms").document(chatroom_id)

            this.chatRoomListener = chatRoomRef.addSnapshotListener { value, e ->

                onChatroomRetrieved(value, e)

            }
        }
    }

    suspend fun getUser(userRef: DocumentReference) = userRef
        .get()
        .await()
        .toObject(User::class.java)?.apply {
            this.uid = userRef.id
        }

    private fun onChatroomRetrieved(value: DocumentSnapshot?, e: FirebaseFirestoreException?) =
            GlobalScope.launch {

                if (e != null || value == null) {
                    Log.w("MyTag", "Listen failed.", e)
                    return@launch
                }

                val chatRoom = value.toObject(ChatRoom::class.java)

                if (chatRoom == null) {
                    Log.w("MyTag", "Conversion failed.", e)
                    return@launch
                }

                chatRoom.messages?.size?.let {

                    if (chatRoom.seen == null) {
                        chatRoom.seen = mutableMapOf( currentUser.uid to it.minus(1))
                        updateChatRoom(value.reference, chatRoom)
                    } else if (chatRoom.seen!![currentUser.uid] != it.minus(1)) {
                        chatRoom.seen!![currentUser.uid] = it.minus(1)
                        updateChatRoom(value.reference, chatRoom)
                    }


                }

                val users = ArrayList<User>()

                val getUserList = async {
                    chatRoom.users?.let {
                        for (userRef in it) {
                            getUser(userRef)?.let { it1 -> users.add(it1) }
                        }
                    }
                }

                getUserList.await()

                chatRoom.userModels = users

                runOnUiThread {
                    updateChatView(chatRoom)
                }
        }

    private fun updateChatRoom(chatRoomRef: DocumentReference, chatRoom: ChatRoom) {
        chatRoomRef.set(chatRoom, SetOptions.merge())
    }

    private fun updateChatView(chatRoom: ChatRoom) {

        actionBar.title = chatRoom.userModels?.find { !it.uid.equals(currentUser.uid) }?.displayName
            ?: getString(R.string.unknownUser)

        this.viewAdapter.updateList(chatRoom)
        this.recyclerView.scrollToPosition(this.viewAdapter.itemCount - 1)

    }
}