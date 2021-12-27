package com.example.registration

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chauthai.swipereveallayout.SwipeRevealLayout
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.example.registration.Models.Consts
import com.example.registration.Models.User
import com.firebase.ui.database.FirebaseListAdapter
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import androidx.recyclerview.widget.DividerItemDecoration
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.graphics.drawable.ClipDrawable.HORIZONTAL
import com.example.registration.Models.Consts.Companion.BLOCK
import com.example.registration.Models.Consts.Companion.UNBLOCK

class MapActivity : AppCompatActivity() {

    lateinit var listOfUsers: RecyclerView
    lateinit var adapter: FirebaseRecyclerAdapter<User, UsersViewHolder>
    lateinit var users: DatabaseReference
    var auth: FirebaseAuth = FirebaseAuth.getInstance()
    var currentUserUid : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        listOfUsers = findViewById(R.id.list_of_users)
        listOfUsers.layoutManager = LinearLayoutManager(this)
        listOfUsers.setHasFixedSize(true)

        currentUserUid = getIntent().getStringExtra(Consts.CURRENT_USER_UID).toString()

        val viewBinderHelper = ViewBinderHelper()
        val itemDecor = DividerItemDecoration(applicationContext, HORIZONTAL)
        listOfUsers.addItemDecoration(itemDecor)

        users =  FirebaseDatabase.getInstance().getReference("Users")
        adapter = object:FirebaseRecyclerAdapter<User, UsersViewHolder>(User::class.java,R.layout.swipe_list_item,
            UsersViewHolder::class.java,users){
            override fun populateViewHolder(v: UsersViewHolder?, model: User?, position: Int) {
                v?.userUid?.text = adapter.getRef(position).key
                v?.userName?.text = model?.name
                v?.userEmail?.text = model?.email
                v?.userRegistrationDate?.text = model?.regDate
                v?.userLastSignDate?.text = model?.lastSignInDate
                v?.userStatus?.text = model?.status.toString()
                when (model?.status.toString()){
                    Consts.BLOCKED_STATUS -> v?.swipeBlock?.text = UNBLOCK;
                    else -> v?.swipeBlock?.text = BLOCK;
                }
            }
            override fun onBindViewHolder(viewHolder: UsersViewHolder, position: Int) {
                super.onBindViewHolder(viewHolder, position)
                viewBinderHelper.bind(viewHolder.swipeRevealLayout, adapter.getRef(position).key)
                viewHolder.swipeBlock.setOnClickListener{
                    blockUser(position)
                }
                viewHolder.swipeDelete.setOnClickListener{
                    deleteUserWithConfirmation(position)
                }
            }
            fun deleteUserWithConfirmation(position:Int){
                val confirmation = AlertDialog.Builder(this@MapActivity)
                confirmation.setTitle("Delete user")
                confirmation.setMessage("Are you sure?")
                confirmation.setNegativeButton("No"){dialog, which->dialog.dismiss();}
                confirmation.setPositiveButton("Yes"){dialog, which->
                    run{
                        val itemRef = adapter.getRef(position)
                        itemRef.removeValue()
                        Toast.makeText(applicationContext,"User is deleted.", Toast.LENGTH_LONG).show()
                    }}
                confirmation.create()
                confirmation.show()
            }
            fun blockUser(position:Int){
                val itemRef = adapter.getRef(position)
                itemRef?.let {it1 ->
                    it1.child("status").get().addOnSuccessListener {
                        when (it.value.toString()) {
                            Consts.BLOCKED_STATUS -> {
                                it1.child("status").setValue("Unblocked")
                                Toast.makeText(applicationContext,"User is unblocked.", Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                it1.child("status").setValue("Blocked")
                                Toast.makeText(applicationContext,"User is blocked.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
        listOfUsers.adapter = adapter

        val logOutButton:Button = findViewById(R.id.button_log_out)
        logOutButton.setOnClickListener{
            auth.signOut()
            updateUIback()
        }

        val vListener = object: ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                a@ for(ds in dataSnapshot.children) {
                    var user = ds.getValue(User::class.java)
                    var userN = User();
                    user?.let {userN=it}
                        if(userN.status=="Blocked") {
                            if (ds.key == currentUserUid) {
                                //Toast.makeText(applicationContext, "You are signed out because you have been blocked by another user", Toast.LENGTH_LONG)
                                auth.signOut()
                                Log.i("user","currentUserUid $currentUserUid")
                                currentUserUid=""
                                updateUIback()
                                break@a
                            }
                        }

                }

            }
            override fun onCancelled(databaseError: DatabaseError) {            }
        }
        users.addValueEventListener(vListener)
    }

     class UsersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userUid: TextView = itemView.findViewById(R.id.user_uid)
        val userName: TextView = itemView.findViewById(R.id.user_name)
        val userEmail:TextView = itemView.findViewById(R.id.user_email)
        val userRegistrationDate:TextView = itemView.findViewById(R.id.user_registration_date)
        val userLastSignDate:TextView = itemView.findViewById(R.id.user_last_sign_date)
        val userStatus:TextView = itemView.findViewById(R.id.user_status)
        val swipeDelete:TextView = itemView.findViewById(R.id.delete_swipe)
        val swipeBlock:TextView = itemView.findViewById(R.id.block_swipe)
        val swipeRevealLayout: SwipeRevealLayout = itemView.findViewById(R.id.swipe_layout)
    }

     private fun updateUIback(){
        val changeBack = Intent(this@MapActivity,MainActivity::class.java )
         changeBack.addFlags(FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(changeBack);
        finish()
    }

}
