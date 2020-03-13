package com.iseokchan.dorandoran

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.iseokchan.dorandoran.adapters.ChatRoomAdapter
import com.iseokchan.dorandoran.models.ChatRoom
import com.iseokchan.dorandoran.models.User
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

            itemClick = object : ChatRoomAdapter.onItemClicked {

                override fun onChatRoomClicked(view: View, position: Int, chatroom: ChatRoom) {

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

        val ab: ActionBar? = supportActionBar

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chatlist_actionbar_menus, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            R.id.action_new_chatroom -> {
                showNewChatRoomDialog()
                true
            }
            R.id.action_sign_out -> {
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showNewChatRoomDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("새 채팅")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        builder.setView(input)

        builder.setPositiveButton("OK",
            DialogInterface.OnClickListener { dialog, which ->
                val searchEmail = input.text.toString()

                // firstly select user

                val usersRef = rootRef.collection("users")
                usersRef.whereEqualTo("email", searchEmail).get().addOnCompleteListener { it ->

                    if(it.isSuccessful) {

                        val documents = it.result?.documents

                        documents?.let {

                            if(it.size == 0) {
                                Snackbar.make(chatListLayout, R.string.cannotFindUser, Snackbar.LENGTH_SHORT).show()
                                return@let
                            }

                            createNewChatRoom(it[0].toObject(User::class.java), it[0].reference)



                        } ?: Snackbar.make(chatListLayout, R.string.cannotFindUser, Snackbar.LENGTH_SHORT).show()


                    } else {

                        Snackbar.make(chatListLayout, R.string.cannotFindUser, Snackbar.LENGTH_SHORT).show()

                    }

                }


            })
        builder.setNegativeButton("Cancel",
            DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })

        builder.show()
    }

    private fun createNewChatRoom(friend:User?, friendRef: DocumentReference) {

        val myRef = rootRef.collection("users").document(currentUser.uid)
        val chatRoomRef = rootRef.collection("chatrooms")

        chatRoomRef
            .add(ChatRoom(

                displayName = friend?.displayName ?: "알 수 없는 친구",
                users = ArrayList<DocumentReference>(2).apply {
                    add(myRef)
                    add(friendRef)
                }
            ))
            .addOnSuccessListener {
                Snackbar.make(chatListLayout, R.string.chatRoomCreated, Snackbar.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("MY TAG", e.message)
                Snackbar.make(chatListLayout, R.string.cannotFindUser, Snackbar.LENGTH_SHORT).show()
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
            val intent = Intent(this, LoginActivity::class.java)
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
