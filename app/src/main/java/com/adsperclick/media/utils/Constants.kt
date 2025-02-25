package com.adsperclick.media.utils

object Constants {

    const val TOKEN_FOR_PREFS = "token_prefs"
    const val USER_IDENTITY = "user_identity"
    const val IS_USER_SIGNED_IN = "is_user_signed_in"
    const val EMPTY = ""
    const val SPACE = " "


    object ROLE{
        const val CLIENT = 1        // Can use Enum instead
        const val EMPLOYEE = 2
        const val MANAGER = 3
        const val ADMIN = 4
    }

    object SEND_TO{
        const val CLIENT = 1
        const val EMPLOYEE = 2
        const val BOTH = 70
    }


    const val USER_TYPE_SEMI_CAPS="UserType"
    const val USER_NAME="UserName"
    const val EMPLOYEES_SEMI_CAPS = "Employees"
    const val CLIENTS_SEMI_CAPS = "Clients"
    const val SERVICES_SEMI_CAPS = "Services"
    const val COMPANIES_SEMI_CAPS = "Companies"
    const val USER_TYPE="UserType"


    object MSG_TYPE{
        const val TEXT = 180
        const val IMG_URL = 200
        const val PDF_DOC = 220
        const val VIDEO = 240
    }


    const val CLICKED_GROUP = "Jis group ko click kia"


    object DB{
        const val USERS = "users"
        const val GROUPS = "groups"
        const val NOTIFICATIONS = "notifications"
        const val MESSAGES = "groups"
        const val COMPANY = "companies"
        const val SERVICE = "services"
    }

    object TXT_MSG_TYPE{
        const val SINGLE_MSG = 1
        const val FIRST_MSG = 2
        const val MIDDLE_MSG = 3
        const val LAST_MSG = 4
    }
}