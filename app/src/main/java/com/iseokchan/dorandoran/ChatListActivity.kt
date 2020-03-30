package com.iseokchan.dorandoran

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.iseokchan.dorandoran.DoranDoranApplication.Companion.firebaseAuth
import com.iseokchan.dorandoran.DoranDoranApplication.Companion.rootRef
import com.iseokchan.dorandoran.adapters.ChatRoomAdapter
import com.iseokchan.dorandoran.classes.ForceUpdateChecker
import com.iseokchan.dorandoran.models.ChatRoom
import com.iseokchan.dorandoran.models.User
import kotlinx.android.synthetic.main.activity_chatlist.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await


class ChatListActivity : AppCompatActivity() {

    private lateinit var swipeLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: ChatRoomAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    private lateinit var actionBar: ActionBar

    private var chatRoomListListener: ListenerRegistration? = null


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatlist)

        DoranDoranApplication.checkIfLoggedIn {
            DoranDoranApplication().resetToLoginActivity()
        }

        ForceUpdateChecker.with(this)
            .onUpdateNeeded(object : ForceUpdateChecker.OnUpdateNeededListener {
                override fun onUpdateNeeded(updateUrl: String?) {
                    val dialog: AlertDialog =
                        AlertDialog.Builder(this@ChatListActivity)
                            .setTitle(getString(R.string.newVersionAvailable))
                            .setMessage(getString(R.string.pleaseUpdate))
                            .setPositiveButton(getString(R.string.update)) { _, _ ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                            }.setNegativeButton(getString(R.string.noThanks)) { _, _ ->
                                ActivityCompat.finishAffinity(this@ChatListActivity)
                            }.create()
                    dialog.show()
                }

            }).check()

        viewManager = LinearLayoutManager(this)
        viewAdapter = ChatRoomAdapter(ArrayList<ChatRoom>(), null).apply {

            itemClick = object : ChatRoomAdapter.onItemClicked {

                override fun onChatRoomClicked(view: View, position: Int, chatRoom: ChatRoom) {

                    val intent = Intent(this@ChatListActivity, ChatActivity::class.java)
                    intent.putExtra("uid", firebaseAuth.currentUser?.uid)
                    intent.putExtra("chatroom_id", chatRoom.id)
                    intent.putExtra("isChatListActivity", true)
                    startActivity(intent)
                }

                override fun onChatRoomLongClicked(view: View, position: Int, chatRoom: ChatRoom) {
                    val colors = arrayOf(getString(R.string.leaveRoom))

                    val builder: AlertDialog.Builder = AlertDialog.Builder(this@ChatListActivity)
                    builder.setTitle(getString(R.string.selectAction))
                    builder.setItems(
                        colors
                    ) { _, which ->
                        when (which) {
                            0 -> { // copy
                                chatRoom.id?.let { leaveRoom(it) }
                            }
                        }
                    }
                    builder.show()
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

        this.actionBar = supportActionBar!!

        swipeLayout = srl_chatrooms

        swipeLayout.setOnRefreshListener {
            getChatRoomsOnce()
        }

        onUserChange(firebaseAuth.currentUser)

    }

    private fun leaveRoom(chatRoom_id: String) {
        rootRef.collection("chatrooms").document(chatRoom_id).update(
            "users",
            FieldValue.arrayRemove(firebaseAuth.currentUser?.uid?.let {
                rootRef.collection("users").document(
                    it
                )
            })
        )
        Toast.makeText(this, getString(R.string.leavedRoom), Toast.LENGTH_SHORT).show()
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

                DoranDoranApplication.signOut(this)

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
        input.hint = getString(R.string.enterOtherUserEmail)
        builder.setView(input)

        builder.setPositiveButton(
            android.R.string.ok
        ) { _, _ ->
            val searchEmail = input.text.toString()

            // firstly select user

            val usersRef = rootRef.collection("users")
            usersRef.whereEqualTo("email", searchEmail).get().addOnCompleteListener { it ->

                if (it.isSuccessful) {

                    val documents = it.result?.documents

                    documents?.let {

                        if (it.size == 0) {
                            Snackbar.make(
                                chatListLayout,
                                R.string.cannotFindUser,
                                Snackbar.LENGTH_SHORT
                            ).show()
                            return@let
                        }

                        val user = it[0].toObject(User::class.java)?.apply {
                            this.uid = it[0].reference.id
                        }

                        createNewChatRoom(user, it[0].reference)


                    } ?: Snackbar.make(
                        chatListLayout,
                        R.string.cannotFindUser,
                        Snackbar.LENGTH_SHORT
                    ).show()


                } else {

                    Snackbar.make(chatListLayout, R.string.cannotFindUser, Snackbar.LENGTH_SHORT)
                        .show()

                }

            }


        }
        builder.setNegativeButton(android.R.string.cancel
        ) { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun createNewChatRoom(friend: User?, friendRef: DocumentReference) {

        val myRef = firebaseAuth.currentUser?.uid?.let { rootRef.collection("users").document(it) } ?: return
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
                Snackbar.make(chatListLayout, R.string.chatRoomCreated, Snackbar.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener { e ->
                Snackbar.make(chatListLayout, R.string.cannotFindUser, Snackbar.LENGTH_SHORT).show()
            }

    }

    private fun onUserChange(user: FirebaseUser?) {

        if (user != null) {
            addChatroomSnapshotListener()
            getChatRoomsOnce()
        }

    }

    private fun updateChatRoomView(chatRooms: ArrayList<ChatRoom>) {

        this.viewAdapter.updateList(chatRooms, firebaseAuth.currentUser?.uid)
        swipeLayout.isRefreshing = false

    }

    private fun getChatRoomsOnce() {

        val uidRef = rootRef.collection("chatrooms")
        val userRef = firebaseAuth.currentUser?.uid?.let { rootRef.collection("users").document(it) } ?: return

        swipeLayout.isRefreshing = true

        uidRef.whereArrayContains("users", userRef).get().addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                onChatroomRetrieved(task.result!!)
            } else {
                swipeLayout.isRefreshing = false
            }
        }


    }

    private fun addChatroomSnapshotListener() {

        val uidRef = rootRef.collection("chatrooms")
        val userRef = firebaseAuth.currentUser?.uid?.let { rootRef.collection("users").document(it) } ?: return

        if (this.chatRoomListListener == null) {
            this.chatRoomListListener =
                uidRef.whereArrayContains("users", userRef).addSnapshotListener { value, e ->
                    onChatroomRetrieved(value, e)
                }
        }

    }

    private fun onChatroomRetrieved(value: QuerySnapshot?, e: FirebaseFirestoreException?) {

        if (e != null) {
            return
        }

        value?.let { onChatroomRetrieved(it) }

    }

    private fun onChatroomRetrieved(documents: QuerySnapshot) = GlobalScope.launch {

        val chatRooms = ArrayList<ChatRoom>()
        val userIdRefMap = HashMap<String, DocumentReference>()

        for (document in documents) {

            val chatRoomObj = document.toObject(ChatRoom::class.java).apply {
                this.id = document.id
            }

            chatRooms.add(chatRoomObj)

            chatRoomObj.users?.forEach {
                userIdRefMap.put(it.id, it)
            }

        }

        //유저 불러오기

        val userDeferredList = ArrayList<Deferred<User?>>()

        userIdRefMap.forEach { (_, u) ->
            userDeferredList.add(GlobalScope.async {
                u.get().await().toObject(User::class.java).apply {
                    this?.uid = u.id
                }
            })
        }

        val userList: List<User> = userDeferredList.awaitAll().filterNotNull()
        val userIdModelMap: Map<String, User> = userList.associateBy({ it.uid!! }, { it })

        // 유저 매핑

        for(chatRoom in chatRooms) {
            chatRoom.userModels = ArrayList()
            chatRoom.users?.forEach { ref ->
                if(userIdModelMap.containsKey(ref.id))
                    chatRoom.userModels?.add(userIdModelMap.getOrElse(ref.id) { return@forEach })
            }
        }

        // 정렬
        chatRooms.sortBy {
            if ( it.messages != null && it.messages.size > 0 ) {
                it.messages.last().createdAt?.time ?: -1
            } else {
                it.createdAt?.time ?: -1
            }
        }

        chatRooms.reverse()

        runOnUiThread {
            updateChatRoomView(chatRooms)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        this.chatRoomListListener?.remove()
    }


}
