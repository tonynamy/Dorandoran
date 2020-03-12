package com.iseokchan.dorandoran

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.iseokchan.dorandoran.adapters.ChatRoomAdapter
import com.iseokchan.dorandoran.models.ChatRoom
import kotlinx.android.synthetic.main.activity_chatlist.*


class ChatListActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var rootRef: FirebaseFirestore
    private lateinit var currentUser: FirebaseUser

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: ChatRoomAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatlist)

        auth = FirebaseAuth.getInstance()
        rootRef = FirebaseFirestore.getInstance()

        viewManager = LinearLayoutManager(this)
        viewAdapter = ChatRoomAdapter(ArrayList<ChatRoom>().apply {
            add(
                ChatRoom(
                    "1"

                )
            )

        }).apply {

            itemClick = object : ChatRoomAdapter.ItemClick {

                override fun onClick(view: View, position: Int, chatroom: ChatRoom) {

                    val intent = Intent(this@ChatListActivity, ChatActivity::class.java)
                    intent.putExtra("uid", currentUser.uid)
                    intent.putExtra("chatroom_id", chatroom.id)
                    startActivity(intent)

                }

            }

        }

        recyclerView = rv_chatRooms.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter

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

    private fun updateUI(user: FirebaseUser?) {

        if (user !== null) {

            Snackbar.make(chatListLayout, R.string.loginSuccess, Snackbar.LENGTH_SHORT).show()
            getChatRooms()

        } else {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    private fun updateChatRoomView(chatRooms: ArrayList<ChatRoom>) {

        this.viewAdapter.updateList(chatRooms)

    }

    private fun getChatRooms() {

        val uidRef = rootRef.collection("chatrooms")
        val userRef = rootRef.collection("users").document(currentUser.uid)

        Snackbar.make(chatListLayout, R.string.loadingChatRooms, Snackbar.LENGTH_SHORT).show()

        uidRef.whereArrayContains("users", userRef).addSnapshotListener { value, e ->

            if (e != null) {
                Log.w("MyTag", "Listen failed.", e)
                return@addSnapshotListener
            }
            val documents = value!!

            val chatRooms = ArrayList<ChatRoom>()

            for (document in documents) {

                Log.d("MyTag", document.id + " => " + document.data)

                val obj: ChatRoom = document.toObject(ChatRoom::class.java)
                obj.id = document.id
                chatRooms.add(obj)
            }

            updateChatRoomView(chatRooms)
        }


    }


}
