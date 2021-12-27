package com.example.registration

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import com.example.registration.Models.User
import com.example.registration.Models.Consts
import com.example.registration.Models.Consts.Companion.BLOCKED_STATUS
import com.example.registration.Models.Consts.Companion.CURRENT_USER_UID
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.rengwuxian.materialedittext.MaterialEditText
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var buttonSignIn: Button
    lateinit var buttonRegister: Button
    lateinit var root:RelativeLayout
    lateinit var auth: FirebaseAuth
    lateinit var db: FirebaseDatabase
    lateinit var users: DatabaseReference
    var currentUserUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonSignIn = findViewById(R.id.buttonSignIn)
        buttonRegister = findViewById(R.id.buttonRegister)
        root  = findViewById(R.id.root_element)
        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()
        users =  db.getReference("Users")

        buttonRegister.setOnClickListener(){
            showRegisterWindow()
        }
        buttonSignIn.setOnClickListener(){
            showSignInWindow()
        }
    }

    override fun onRestart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) auth.checkBlockingAndEnter(users)
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) auth.checkBlockingAndEnter(users)
    }

    private fun showSignInWindow() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Sign In")
        dialog.setMessage("Input data for registration")
        var inflater = LayoutInflater.from(this)
        var signInWindow = inflater.inflate(R.layout.sign_in_window,null)
        dialog.setView(signInWindow)

        var email:MaterialEditText = signInWindow.findViewById(R.id.emailField)
        var password:MaterialEditText = signInWindow.findViewById(R.id.passwordField)

        dialog.setNegativeButton("Cancel"){dialog, which->dialog.dismiss()}
        dialog.setPositiveButton("Sign In"){dialog, which->
            run {
                if (TextUtils.isEmpty(email.text.toString())) {
                    Snackbar.make(root, "Enter your E-mail", Snackbar.LENGTH_LONG).show()
                    return@run
                }
                if (password.text.toString().length<5) {
                    Snackbar.make(root, "The password should be more than 6 characters", Snackbar.LENGTH_LONG).show()
                    return@run
                }
                auth.authorizeUser(email.text.toString(),password.text.toString(),users)
            }
        }
        dialog.show()
    }

    private fun showRegisterWindow() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Register")
        dialog.setMessage("Input data for registration")
        val inflater = LayoutInflater.from(this)
        val registerWindow = inflater.inflate(R.layout.register_window,null)
        dialog.setView(registerWindow)

        val email:MaterialEditText = registerWindow.findViewById(R.id.emailField)
        val name:MaterialEditText = registerWindow.findViewById(R.id.nameField)
        val password:MaterialEditText = registerWindow.findViewById(R.id.passwordField)

        dialog.setNegativeButton("Cancel"){dialog, which->dialog.dismiss()}
        dialog.setPositiveButton("Add"){dialog, which->
            run {
                if (TextUtils.isEmpty(email.text.toString())) {
                    Snackbar.make(root, "Enter your E-mail", Snackbar.LENGTH_LONG).show()
                    return@run
                }
                if (TextUtils.isEmpty(name.text.toString())) {
                    Snackbar.make(root, "Enter your name", Snackbar.LENGTH_LONG).show()
                    return@run
                }
                if (password.text.toString().length<5) {
                    Snackbar.make(root, "The password should be more than 6 characters", Snackbar.LENGTH_LONG).show()
                    return@run
                }
                auth.createUserWithEmailAndPassword(email.text.toString(),password.text.toString())
                    .addOnSuccessListener(OnSuccessListener {
                        FirebaseAuth.getInstance().currentUser?.uid?.let { it1 ->
                            var user = User(email.text.toString(),name.text.toString(),password.text.toString(),getCurrentDateTime().toString(Consts.SDT))
                            users.child(it1)
                                .setValue(user)
                                .addOnSuccessListener{Snackbar.make(root,"User added!",Snackbar.LENGTH_LONG).show()}
                        }
                        auth.authorizeUser(email.text.toString(),password.text.toString(),users)
                    })
                    .addOnFailureListener{
                        Snackbar.make(root,"Error of registration: ${it.message}",Snackbar.LENGTH_LONG).show()
                    }
            }
        }
    dialog.show()
    }

    fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
       val formatter = SimpleDateFormat(format, locale)
       return formatter.format(this)
    }

    fun getCurrentDateTime(): Date {
        return Calendar.getInstance().time
    }

    private fun updateUI(){
        val changePage = Intent(this@MainActivity ,MapActivity::class.java)
        changePage.putExtra(CURRENT_USER_UID,currentUserUid)
        changePage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        changePage.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        Log.i("activity","Start activity: MapActivity")
        startActivity(changePage);
        finish()
    }

    private fun FirebaseAuth.authorizeUser(email:String, password:String, database:DatabaseReference){
        this.signInWithEmailAndPassword(email,password)
            .addOnSuccessListener {
                this.checkBlockingAndEnter(users)
                Log.i("firebase","User ${this.currentUser}, id ${this.currentUser?.uid} signed in")
            }
            .addOnFailureListener{
                Snackbar.make(root,"Error of authorization. ${it.message}",Snackbar.LENGTH_LONG).show()}
    }

    fun FirebaseAuth.checkBlockingAndEnter(database: DatabaseReference){
        this.currentUser?.let {it1 ->
            users.child(it1.uid).child("status").get().addOnSuccessListener{
                when(it.value.toString()) {
                    BLOCKED_STATUS -> auth.signOutWithBlockingMessage()
                    else-> enterAndUpdateSignInTime(database, it1.uid)
                }
            }
                .addOnFailureListener{task -> enterAndUpdateSignInTime(database, it1.uid)}
        }
    }

    fun enterAndUpdateSignInTime(database:DatabaseReference, uid:String){
        database.child(uid).child("lastSignInDate")
            .setValue(getCurrentDateTime().toString(Consts.SDT))
        currentUserUid = auth.currentUser?.uid
        updateUI()
    }

    fun FirebaseAuth.signOutWithBlockingMessage() {
        this.signOut()
        Snackbar.make(root, "Cant sign in: You are blocked by another user.", Snackbar.LENGTH_LONG).show()
    }
}