package com.adsperclick.media.aa_Notes

/*


 Saumya Kumar Thakur
 This file contain notes to revise this project in detail
 Notes are divided into 3 sections
    - BUSINESS LOGIC (Tells us about the task in hand what exactly we have made, what is the problem statement)
    - BACKEND IMPLEMENTATION (FIREBASE NOTES : Explained all the firebase tools used build the backend)
    - ANDROID IMPLEMENTATION (Explained how we have implemented each and every feature in our application)




    -----------------------------------------------BUSINESS-LOGIC------------------------------------------------

    The guy we are working for, lets call him "X", owns a company called AdsPerClick , It is a
    start-up which helps provide reach to its clients (Which are small businessman).
    For example a businessman wants to start selling his product on amazon, earlier it used to be easy
    but nowadays there's a lot of competition in this field, so what AdsPerClick does is help them provide
    proper reach, by creating attractive posters for the product so that it looks attractive on platforms
    like "Amazon", "Meesho" etc. ... It also advertises the product on social media platforms like
    "Instagram" to make the product famous and increase its sales

    X basically needs a chat application, where his employees and clients can chat, send images, share videos,
    or share any type of documents... In this app, we have an "Admin" account, which is owned by "X".
    Only an admin has the power to create a new account, so if any user wants to logs in, only admin can
    create their account and share credentials, then only they'll be able to log in. (To create an admin
    account, we'll need to change the config directly from backend, else there's no way XD)

    What extra an admin can do?
    Ok so our actual "client" here is small businesses right? so we call them "client-company" or
    simply "company"
    Now this Client-company can have several people working in it so we call them "Clients"
    Therefore, each client is linked to a "Client-company", so when creating a client account, we need
    to mention the client-company name to which this "client" is associated

    Therefore a client account is created when we enter client name, (email-Id & password, his
    login credentials) and the company name to which this client is associated.

    When creating an employee account we display a list of companies to select company in which this
    employee is working.... if this employee is not working in any of the existing companies... then
    our admin will need to first create the company and then create the employee account,

    Our admin also has the ability to create new company accounts (When he finds a new client)
    When creating company account, we ask for company-name, company-gst-number ( which is unique
    & registered with the govt. for each company) and we also ask for the "Services subscribed"
    this includes services like "Amazon", "Meesho", "Flipkart" Like where this company wants
    to sell their product exactly..


    Just like Company-accounts, admin also has the power to create new services-account, like right now
    "X" is not working with "Zomato", maybe in future he starts catering for Companies who want to sell
    their product on zomato (Like restaurants) then "X" would want to add "Zomato" to the list of Services
    right? So first "X"  will create a new service called "Zomato", then he will create a new
    company-account for which the services-subscribed would be zomato ... and then "X" would create
    client accounts... now these "clients" are basically members of the client company...

    After creating these client accounts, he can add these clients to a new group-chat or some older
    group-chat, where these-
    members-of-the-company can interact with employees of the company, and they can have conversations
    related to various things like how they'll increase reach or what they'll do, share images,
    documents, videos, etc...
    In groups we also have the feature of group-call where if a group-member (which can be anyone)
    starts a call, everyone in the group will be notified and hear a ringing that a call is ongoing,
    you can join

    // While creating Employee account we're asking for name, email-id, password, Aadhar Card for each
    employee

    // So these are some of the special powers of ADMIN -> Creating accounts for
        - Services (Services like Amazon, meesho)
        - Company (Client company)
        - Employee (Employees of X)
        - Client (Members/representative of the client company who will talk to employees)


    // On the admin account, when he's on "ChatFragment" (Where list of group-chats is visible)
    around DP of each group there will be a ring, this ring will indicate to the admin what is
    the current status of that group, in case there's ongoing conversations in the group, he'll
    see no rings around the DP, in case the last message in group is sent by a client and the
    employee has not responded in the last 1 hour, then a red-ring will be visible around the
    DP of that group
    // If last msg sent by employee & client hasn't responded for 2 hours, then purple ring around DP



    // A user can refer to anyone using the app can be employee or client

    // Admin can also block any user, in that case if a user tries to login he'll get a toast
    that he's blocked or if he's signed in, he'll automatically signed out from the app.
    // In future admin can also unblock that user, and all data of that user will be intact!

    // Each user of the app can setup their own Profile DP and each group can also
    have it's own profile dp (just like whatsapp)

    // Admin also has the power to send broadcast notifications. He can send to all, or send to just
    employees or send to all clients.

    // When a new broadcast notification is sent each user also receive notification on their mobiles

    // A push notification is also sent to all group members when someone sends a message in any group.
    // Utilised FCM (Firebase cloud messaging) to implement this

    Implemented search bar for searching for searching for specific group, user, company or service in
    their respective fragments.

    // chatgpt brief ----->
    //  •	Developed a full-fledged business communication app using Kotlin, MVVM architecture,
    Dagger Hilt, and Firebase (Auth, Firestore, Storage, Crashlytics, Realtime Database).

    //	•	Built a role-based access system, enabling an admin to create and manage accounts for
    services, companies, employees, and clients, ensuring controlled onboarding.

    //	•	Implemented real-time messaging & group calls with Firestore and Firebase Cloud
    Messaging (FCM), allowing seamless communication with notifications and media sharing.

    //	•	Enhanced admin monitoring with visual indicators for conversation status and implemented
     blocking/unblocking functionality for users.

    //	•	Optimized search and notifications, allowing users to quickly find groups, users,
    companies, or services and receive real-time updates via FCM push notifications.





--------------------------------- BACKEND-LOGIC (Firebase NOTES) ---------------------------------------------

    We're using Firebase as backend, (Serverless backend)
    Firebase is Backend-as-a-service (BAAS).
    It helps us build a backend much quicker compared to traditional backends.
    Firebase provides us with a suite of services which are often required in the backend,
    it speeds-up the app development process by providing inbuilt tools for performing various imp
    tasks like authorization, data storage, realtime database, setting up backend security, checking
    crash logs etc.


    We'll go through various firebase tools used in our app

    FIREBASE-AUTH
    Firebase Auth provides us a proper login and signup backend, like in our notes-app we had manually
    set this up, when a user logs in, he'd get a token that he'll use as header-authorization
    to obtain his data from backend right, here all these things are handled internally in a much
    simpler/secure/efficient way.

    Here, once a user logs in his token is generated, and that token "gets renewed internally" without
    us even knowing a thing, all this is done internally for security purpose.
    Also, after we login using firebase auth and we don't log out, we stay logged in just like we want
    in our app, when user explicitly clicks log-out then only he's logged out.
    When we are logged in, with every request we send to the backend, our authorization data is sent!
    We don't even send that data explicitly but our authorization data is sent to backend automatically
    with every request we make to any service like firestore, storage, realtime database etc.

    Later on when we write "Security rules" for firestore or firebase-storage, you'll find this info
    useful as it enhances backend security and prevents unauthorized access to data.


    FIREBASE-STORAGE
    Used for storing large files or documents, can store images, videos, pdfs, etc. In our app we're
    storing all the files in this only.
    How to use it? Write the path where you want to store this file, follow a proper folder structure
    to store the document like SharedInGroup/{groupId}/{fileType}/{filename} ----> Like this it'll be
    stored in a proper file structure, fileType can be video/img/pdf/jpg etc... will be helpful for
    you to later see from firebase console what is present where

    After this upload the URI of the file to firebase-storage, after the file is uploaded we'll get
    a "downloadUrl", now receiving downloadUrl means that our data is saved on server and we can anytime
    from anywhere use this "downloadUrl" to access that data we'll save this downloadUrl somewhere
    coz it can fetch us the document directly from the server. Read "UploadFile()" func in MessagingRepository

    In our case for MessagingFragment (Where chatting happens) we're storing the downloadUrl in the
    message object so that when a user shares an image in a group-chat, it'll use that downloadUrl
    on everyone's phone to display that image using Glide.

    NOTE: Right now we've written storage-security-rules, to ensure that any document can only be
    accessed by signed-in user, so u can't simply paste the downloadUrl on chrome and view the data,
    if that security-rule was not there, u could've done that!

    FIRESTORE

    Firestore is a cloud-hosted realtime database, it implements "web-sockets" internally to respond
    in realtime helps in building apps requiring realtime data transfer for example chat implementation
    on our application.

    Firestore uses a No-SQL database, (Which means schema-less architecture, which means there are
    no tables... data is stored like some JSON stuff)

    JUST FYI(In Supabase (A firebase alternative there's POSTGRE-SQL based database, which is more
    like the industry standard))

Firestore vs Realtime Database
	•	Firestore is often compared to Firebase Realtime Database, but
	    Firestore provides stronger querying capabilities and automatic indexing, making it more
	    scalable for complex applications.


    In firestore internally indexing is applied, that is why when applying some complex queries using
    firestore, you'll get error!! In that case go to error-logs and check for firestore indexing,
    you'll basically be required to build the indexing then only that complex/ mildly complex query
    will run. "Go to Indexes on 'firestore' console"

    Also I don't know y but if u make a query and it returns a 100 documents, or you make a query
    which returns 100,00 documents, the same computation at firestore's end would be required.

    Data organisation in firestore:
    In firestore we have "Collections" and "Documents",
    Each Collection can hold several documents.. And each document can have two types of things
    1. Fields (The various fields which contain the actual data, like string/number/Boolean/Array)
    2. SubCollection (Each document can have a subCollection inside of it, this subCollection can
                      further have documents and data and then subCollection also, like this tree
                      like structure is built)

    NOTE THAT FIRESTORE QUERIES DON'T HAVE CASCADING EFFECT (This is very important info for
    structuring firestore.

    Now, when you make a request and "get()" a document from backend, only the "Fields" related to
    that document is obtained!

    Similarly, when you put a listener on a particular document, then that listener will ONLY be
    triggered when a "Field" inside that document is changed, not when a subCollection has some
    changes. To listen to changes in that subCollection you have to put a separate listener on that
    subCollection.

    We used this information to control listening... In case of "groups" collection the documents are
    "groupId" which have various fields, but "GroupMembersLastSeenTime" is stored in a separate
    subCollection! That is intentionally done to prevent listening to changes in
    "GroupMembersLastSeenTime"
    Because that field is very frequently changed & I didn't want unnecessary listener triggers to
    slow down my app!

    ✅ Queries in Firestore do not fetch subcollections—you must query them separately.
    ✅ Automatic indexing helps with performance, but complex queries require manual indexing.
    ✅ Firestore charges based on document reads, writes, and storage—not computation time.



    --------------- BELOW ARE MY NOTES FROM FIRESTORE RULES ON FIREBASE CONSOLE -------------------------


    // Go through this to understand firestore rules, why we need them & how to make them

    rules_version = '2';        // Code

// NOTES :
// WHY DO WE NEED FIRESTORE RULES?
// Firestore rules is like a "bouncer" outside of night-clubs, it prevents someone to read or write
// data if they are not supposed to, it sets up a check on the backend, to prevent users from doing
// un-allowed reads or write...

// Now you ask what is the need for this?
// Ans : E.g. The notification listing page, has list of notifications, now those notifications can
// only be written by an admin right? Now on the android app I have put a check that
// notification-creation fragment cannot be accessed by anyone other than Admin right,
// But what if on the IOS app there might be some misunderstanding or glitch because of which
// notification creation fragment is visible and any user can now write a notification and send to
// backend, this is where backend rules come into play, "Client-devices" are much easier to hack
// or manipulate requests... to prevent un-authorized actions and hacking... we use backend rules...
// to make sure our DB is secure, now we could've put several rules but we're keeping it basic

// Read rule for reading and writing of notification

// Now for accessing firestore or most of the things in our app, we have the condition for user sign-in
// If user is signed in he can thereafter only access the data of app... so I have simply put a check
// "isAuthenticated()" for accessing all the documents!!


// STUDY MATERIAL
// : OPERATIONS
// There are basically two firestore operations a user can perform "read" and "write"
// "write" can be further split into "create", "delete", "update" , you can put up separate
// rules for creation deletion and updation of documents depending on use case (e.g. u want to
// allow everyone to write msgs in group, but the ability to delete a msg should only rest with
// the admin then that's how you write the rule)

// :  WILD CARD
// " {} " ---> is a wildcard, it refers to "all documents or things of that type"
// when we write match /users/{userId}  the thing in that "{}" is a variable refering to all
// the documents inside the "users" collection!! So "userId" refers to all the documents inside
// "users" collection... Now we can put up a check like if userId is this or that then only allow
// reading or writing should be allowed etc etc...
// in the begining you'll see		 "match /databases/{database}/documents"
// Now in firestore, one can create "multiple" databases! We are using single db or most ppl use
// single db only but this facility is there
// now when we type "/databases/{database}" , the "database" variable will refer to our database
// which has all our data stored...


// IMPORTANT
// Notes : Firestore rules are not cascading, so if you are writting one for a
// document, then if there's a "subCollection" inside that document, it won't
// apply to that subCollection!
// THIS RULE IS APPLICABLE EVERYTIME AND REMEMBER IT BY HEART
// Even on android side, when we "get" a document, we only get "Fields" of that document (Fields can
// be number/string/array etc...) but not a "subCollection", a subCollection inside a document
// is treated like a separate entity
// Another example: Even in case where we put up a listener on a list of documents.. the
// listener will be triggered if a "Field" inside that document is triggered, but it won't be
// triggered when a "subCollection" inside would be triggered
// That is why in case of "groups" collection, Inside each document we've subCollections
// called "LastSeenTimeEachMembers" and "call_log", we're not directly keeping them as fields
// because then changes in these things will unnecessary trigger the "realTimeListener"
//  we have documentId as groupId, now when user is in
// 10 groups, then his phone on "ChatFragment" will continuously listen to changes in "group Object"
// of all those 10 groups, and the groupObject is changed when "lastSentMsg" is changed, that
// change will trigger the listener which is a desirable behaviour, but we also are saving
// last-seen-time of each user inside that group, not in the groupObject but by using a separate
// subcollection so that listener does not trigger when any group member simply checks out a group
// by entering MessagingFragment That subCollection "lastSeenTimeForEachUser" is very frequently
// updated, so didn't want that to make my UI less responsive by frequent listener triggers..


// So when writing rules .. u know that u match /collectionName/{documentId} and using that
// "documentId" u can fetch all the intricate details inside that document and decide what things
// to allow to what users, just like I used "hasRole()" function to make a rule that only admin
// can write a notification, so even if in future, the client makes a website, backend is robust
// enough to prevent anyone from writing notifications except admin :)


        ----------------------------------------- <CODE> ----------------------------------------------
service cloud.firestore {
  match /databases/{database}/documents {

    function isAuthenticated(){
    	return request.auth != null
    }

    function hasRole(role) {
      return get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == role;
    }

    // match /{document=**} {
    //   allow read, write;
    // }

    match /notifications/{notificationId} {
    	allow read: if isAuthenticated();
      allow write: if hasRole(4);			// Role-4 is the role of "Admin", which means only admin can write
      																// in this document :)
    }

  match /config/{configName} {
      allow read;
      allow write: if !(configName == "minAppLevel");
  }


    match /companies/{companyId} {
    	allow read : if isAuthenticated();
      allow write: if isAuthenticated();
    }

    match /services/{serviceId} {
    	allow read : if isAuthenticated();
      allow write: if isAuthenticated();
    }

    match /users/{userId} {
    	allow read : if isAuthenticated();
      allow write: if isAuthenticated();
    }


    // GROUPS-COLLECTION

    // To handle the data part of each document
    match /groups/{groupId} {
    	allow read : if isAuthenticated() && request.time < timestamp.date(2025, 5, 31);
      allow write: if isAuthenticated();    }


    // To handle the subCollection "GroupMembersLastSeenTime" of each document
    match /groups/{groupId}/GroupMembersLastSeenTime/{lastSeenTime} {
    	allow read : if isAuthenticated();
      allow write: if isAuthenticated();    }

    // To handle the subCollection "call_log" of each document
    match /groups/{groupId}/call_log/{call_id} {
    	allow read : if isAuthenticated();
      allow write: if isAuthenticated();    }

    // NEW RULE: Allow collectionGroup queries THIS IS IMPORTANT, Remember to handle collection
    groups, which means when you are querying all the collections with the same name on a database

    // At the same level two collections can't have the same name... but! If you have several
    documents in a collection... and each document has a subCollection with the name
    "GroupMembersLastSeenTime", then if u wanna gather data of all the subCollections at the same time,
    u can use "collectionGroup", it allows accessing all the collection in the database with the same
    name simultaneously... read "fetchLastSeenTimeForEachUserInEachGroup()" function in ChatRepository
    "{path=**}" means "any path" because this "GroupMembersLastSeenTime" is a subCollection which can
    be present at multiple places (because it contains last seen time of users in a group), so for
    each group Document this subCollection will be there
    match /{path=**}/GroupMembersLastSeenTime/{lastSeenTime} {
      allow read;
    }


    // MESSAGES-COLLECTION

    // To handle the data part of each document
    match /Messages/{groupId} {
    	allow read : if isAuthenticated();
      allow write: if isAuthenticated();    }

    match /Messages/{groupId}/messages/{messageId} {
    	allow read : if isAuthenticated();
      allow write: if isAuthenticated() && request.time < timestamp.date(2025, 5, 31);    }

  }
}


        ----------------------------------------- </CODE> ----------------------------------------------



    ---- QUESTION I ASKED FROM CHAT GPT----
// what does the below line mean exactly

//CODE :     match /Messages/{groupId}/messages/{messageId}

// does it mean inside "Messages" collection "groupId"
//  document, and {groupId} representing all groupId documents,
//   there's a subCollection "messages" inside that for
//   all {messageId} , does above line of code mean this?
//   what are "{}" exactly for

    ----------------------------------- RESPONSE ---------------------------------------
// 	1.	match /Messages/{groupId}
	// •	Refers to the Messages collection.
	// •	{groupId} is a wildcard representing any document ID inside Messages.
	// •	Example: If there are documents like

  //. Messages/abc123
	//. Messages/xyz789

  //then {messageId} could be msg001, msg002, msg003, etc..
  //this dynamically referes to all documents


//   FULL EXAMPLE
//   match /Messages/{groupId}/messages/{messageId} {
//   allow read, write;
// 	}

//   It means:
// 📌 “Allow read and write on all messages subcollections inside any
// groupId document in the Messages collection.”

// 🔹 Example of Matching Paths:
// 	•	✅ Messages/abc123/messages/msg001
// 	•	✅ Messages/xyz789/messages/msg002
// 	•	✅ Messages/group456/messages/msg003

// 🔴 It does NOT match:
// 	•	❌ Messages/abc123 (because this is a document, not a subcollection)
// 	•	❌ Messages/abc123/GroupMembers/member001 (because it’s in a different subcollection)



    --------------- ABOVE ARE MY NOTES FROM FIRESTORE RULES ON FIREBASE CONSOLE -------------------------




    FIREBASE CLOUD FUNCTIONS

    Firebase cloud functions is basically a layer on top of Google-Cloud-Platform, which means it
    internally utilises GCP.

    They're used to write our backend logic, It is similar to making a backend using NodeJS and then
    deploying it on Vercel or somewhere. The difference is it is more convenient, because we don't
    have to think about backend scaling, storage solutions etc...

    We simply create a NodeJS project, then integrate firebase in it using the terminal (Chat-gpt)
    commands.
    Then inside a NodeJS project a folder will be there called "functions" it contains all our
    firebase related stuff,
    It has index.js file, u can write backend code in it then do "firebase deploy --only functions"
    This line will make sure only the "functions folder code is deployed to firebase, on firebase
    these cloud functions are divided into separate components, and these functions might run on
    separate servers ... (You can also specify location of these servers in the function)
    We using "asia-south2" (Delhi server)

    Now by writing a single line of code all these functions are deployed on servers, this is
    convenience of using firebase cloud functions. Otherwise we'd have to deploy these functions
    one by one..


    // Read the notes on my VS Code... for detailed info (On my M1 mac)


    FIREBASE-CLOUD-MESSAGING

    This is the notification sending and receiving service of firebase, used to implement
    notifications. Backend code is written with Firebase cloud functions & android side using
    Firebase Messaging service. Read them

    FCM HTTP V1 uses "-Google OAuth 2.0-" under the hood
    FCM Legacy is deprecated (was more straight forward but less secure)


    FIREBASE-CRASHLYTICS
    After our code is in production, there's no way to know if the app is crashing on the client's
    device or not, is it working fine on all android versions or crashing in some, to know all this
    we should add the crashlytics sdk to our firebase project so that from firebase console we can
    see if the app is crashing in any device or not.
    It's very similar to App-Center
*/



class CompleteNotes


