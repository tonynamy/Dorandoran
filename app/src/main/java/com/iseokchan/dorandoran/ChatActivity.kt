package com.iseokchan.dorandoran

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.iseokchan.dorandoran.adapters.ChatAdapter
import com.iseokchan.dorandoran.adapters.EmoticonPackAdapter
import com.iseokchan.dorandoran.models.*
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await


class ChatActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var rootRef: FirebaseFirestore
    private lateinit var currentUser: FirebaseUser

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: ChatAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    private lateinit var actionBar: ActionBar
    private lateinit var rootStorage: FirebaseStorage
    private lateinit var rootStorageRef: StorageReference

    private var chatRoomListener: ListenerRegistration? = null

    private var chatroomId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        auth = FirebaseAuth.getInstance()
        rootStorage = FirebaseStorage.getInstance()
        rootStorageRef = rootStorage.reference

        if (!intent.hasExtra("chatroom_id") || auth.currentUser == null) {

            return
        }

        this.currentUser = auth.currentUser!!
        this.chatroomId = intent.getStringExtra("chatroom_id") ?: return

        rootRef = FirebaseFirestore.getInstance()

        viewManager = LinearLayoutManager(this)
        viewAdapter = ChatAdapter(ChatRoom(), this.currentUser.uid).apply {
            callBack = object : ChatAdapter.chatAdapterCallback {
                override fun onMessageLongClicked(view: View, position: Int, chat: Chat) {
                    val colors = arrayOf(getString(R.string.copy))

                    val builder: AlertDialog.Builder = AlertDialog.Builder(this@ChatActivity)
                    builder.setTitle(getString(R.string.selectAction))
                    builder.setItems(
                        colors
                    ) { _, which ->
                        when (which) {
                            0 -> { // copy
                                val clipboard =
                                    getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip =
                                    ClipData.newPlainText(getString(R.string.message), chat.content)
                                clipboard.setPrimaryClip(clip)

                                Toast.makeText(
                                    this@ChatActivity,
                                    getString(R.string.copied),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    builder.show()
                }

                override fun onGlideLoadFin() {
                    recyclerView.postDelayed(
                        {
                            recyclerView.smoothScrollToPosition(
                                this@apply.itemCount.minus(
                                    1
                                )
                            )
                        },
                        100
                    )
                }
            }
        }

        recyclerView = rv_chats.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter

        }

        recyclerView.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom && this.viewAdapter.itemCount > 0) {
                recyclerView.postDelayed(
                    {
                        recyclerView.smoothScrollToPosition(
                            this.viewAdapter.itemCount.minus(
                                1
                            )
                        )
                    },
                    100
                )

            }
        }

        et_message.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus)
                emoticonFooter.visibility = View.GONE
        }

        btn_sendMessage.setOnClickListener {

            val message = et_message.text.toString()
            val chatRoomRef = rootRef.collection("chatrooms").document(chatroomId)
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

        btn_emoticon.setOnClickListener {
            if (emoticonFooter.visibility == View.GONE) {
                et_message.clearFocus()

                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(et_message.windowToken, 0)

                emoticonFooter.visibility = View.VISIBLE
            } else {
                emoticonFooter.visibility = View.GONE
            }
        }

        this.actionBar = supportActionBar!!

        //load emoticons
        loadEmoticons()

        val circularProgressDrawable = CircularProgressDrawable(this)
        circularProgressDrawable.strokeWidth = 5f
        circularProgressDrawable.centerRadius = 30f
        circularProgressDrawable.start()

        emoticonViewPagerLoader.visibility = View.VISIBLE
        emoticonViewPagerLoader.setImageDrawable(circularProgressDrawable)
    }

    private fun sendEmoticon(emoticon: Emoticon) {
        val chatRoomRef = rootRef.collection("chatrooms").document(chatroomId)
        chatRoomRef.update(
            "messages", FieldValue.arrayUnion(
                Chat(
                    uid = currentUser.uid,
                    createdAt = Timestamp.now().toDate(),
                    emoticon = emoticon
                )
            )
        )
    }

    override fun onBackPressed() {
        if (emoticonFooter.visibility == View.VISIBLE) { // HIDE Emoticon View pager
            emoticonFooter.visibility = View.GONE
            return
        }

        if (!ChatListActivity().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            val intent = Intent(this, ChatListActivity::class.java)
            startActivity(intent)
        }
        super.onBackPressed()
    }

    private fun loadEmoticonCallback(task: Task<QuerySnapshot>) = runBlocking {

        val emoticonPacks = ArrayList<EmoticonPack>()

        if (task.isSuccessful && task.result != null) {

            val querySnapshot = task.result

            val emoticonPackDeferreds = arrayListOf<Deferred<List<StorageReference>>>()

            for (document in querySnapshot) {
                emoticonPackDeferreds.add(GlobalScope.async {
                    rootStorage.reference.child("emoticons").child(document.id).listAll()
                        .await().items
                })
                emoticonPacks.add(document.toObject(EmoticonPack::class.java).apply {
                    id = document.id
                })
            }

            val emoticonPackDeferredValue = emoticonPackDeferreds.awaitAll()

            for (i in emoticonPackDeferredValue.indices) {

                val emoticons = arrayListOf<Emoticon>()
                val emoticonsDeferred = arrayListOf<Deferred<String>>()

                for (emoticonStorage in emoticonPackDeferredValue[i]) {
                    emoticons.add(Emoticon(displayName = emoticonStorage.name))
                    emoticonsDeferred.add(GlobalScope.async {
                        emoticonStorage.downloadUrl.await().toString()
                    })
                }

                val emoticonsDeferredValue = emoticonsDeferred.awaitAll()

                for (j in emoticonsDeferredValue.indices) {
                    emoticons[j].url = emoticonsDeferredValue[j]
                }

                emoticonPacks[i].emoticons = emoticons

            }
        }

        runOnUiThread {
            emoticonViewPagerLoader.visibility = View.GONE
            emoticonViewPager.visibility = View.VISIBLE
            emoticonViewPager.adapter = EmoticonPackAdapter(emoticonPacks).apply {
                itemClickCallback = object : EmoticonPackAdapter.onItemClicked {
                    override fun onEmoticonClicked(emoticon: Emoticon) {
                        sendEmoticon(emoticon)
                    }

                }
            }
            emoticonViewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

            viewAdapter.emoticonPacks = emoticonPacks

        }

    }

    private fun loadEmoticons() {

        rootRef.collection("emoticons").get().addOnCompleteListener {
            GlobalScope.launch {
                loadEmoticonCallback(it)
            }
        }


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_actionbar_menus, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            R.id.leave_room -> {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.leaveRoom))
                    .setMessage(getString(R.string.leaveRoomConfirm))
                    .setPositiveButton(
                        android.R.string.yes
                    ) { _, _ ->
                        leaveRoom()
                    }
                    .setNegativeButton(android.R.string.no, null).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    public override fun onResume() {
        super.onResume()

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

    override fun onStop() {
        super.onStop()
        this.chatRoomListener?.remove()
        this.chatRoomListener = null
    }

    private fun leaveRoom() {
        rootRef.collection("chatrooms").document(chatroomId).update(
            "users",
            FieldValue.arrayRemove(rootRef.collection("users").document(currentUser.uid))
        )
        Toast.makeText(this, getString(R.string.leavedRoom), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun addSnapshotListener() {

        if (this.chatRoomListener == null) {
            val chatRoomRef = rootRef.collection("chatrooms").document(chatroomId)

            this.chatRoomListener = chatRoomRef.addSnapshotListener { value, e ->

                onChatRoomRetrieved(value, e)

            }
        }
    }

    private fun onChatRoomRetrieved(value: DocumentSnapshot?, e: FirebaseFirestoreException?) =
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
                updateChatRoom(
                    value.reference,
                    mutableMapOf("seen" to mapOf(currentUser.uid to it.minus(1)))
                )

            }

            val userDeferredList = ArrayList<Deferred<User?>>()

            chatRoom.users?.forEach {
                userDeferredList.add(async {
                    it.get().await().toObject(User::class.java)?.apply {
                        this.uid = it.id
                    }
                })
            }

            val users: List<User> = userDeferredList.awaitAll().filterNotNull()

            users.forEach {
                chatRoom.userModels?.add(it)
            }

            runOnUiThread {
                updateChatView(chatRoom)
            }
        }

    private fun updateChatRoom(chatRoomRef: DocumentReference, value: Any) {
        chatRoomRef.set(value, SetOptions.merge())
    }

    private fun updateChatView(chatRoom: ChatRoom) {

        actionBar.title = chatRoom.userModels?.find { !it.uid.equals(currentUser.uid) }?.displayName
            ?: getString(R.string.unknownUser)

        this.viewAdapter.updateList(chatRoom)
        this.recyclerView.scrollToPosition(this.viewAdapter.itemCount - 1)

    }
}