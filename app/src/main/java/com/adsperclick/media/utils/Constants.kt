package com.adsperclick.media.utils

import android.Manifest
import android.os.Build
import android.provider.MediaStore.Video

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
    const val USER_IMAGE="UserImage"
    const val EMPLOYEES_SEMI_CAPS = "Employees"
    const val CLIENTS_SEMI_CAPS = "Clients"
    const val SERVICES_SEMI_CAPS = "Services"
    const val COMPANIES_SEMI_CAPS = "Companies"
    const val USER_TYPE="UserType"
    const val USER_ID="UserId"
    const val GROUP_PROFILE = "GroupProfile"
    const val EMPLOYEE_SINGULAR = "Employee"
    const val COMPANY_SINGULAR = "Company"
    const val GROUP_ID = "GroupId"


    object MSG_TYPE{
        const val TEXT = 180
        const val IMG_URL = 200
        const val PDF_DOC = 220
        const val VIDEO = 240
    }


    const val CLICKED_GROUP = "Jis group ko click kia"

    const val APPLICATION_PDF = "application/pdf"
    const val IMAGE = "image/*"
    const val JPG = ".jpg"
    const val FILES = "files"
    const val yyyyMMdd_HHmmss = "yyyyMMdd_HHmmss"
    const val BACK_SLASH = "/"
    const val DASH = "-"
    const val CAMERA_VISIBLE = "camera"
    const val PDF_VISIBLE = "Doc"
    const val GALLERY_VISIBLE = "gallery"
    const val VIDEO_VISIBLE = "video"
    const val DELETE_VISIBLE = "delete"

    val REQUIRED_PERMISSIONS_CAMERA  = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            arrayListOf(
                Manifest.permission.CAMERA
            )
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            arrayListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        }
        else -> {
            arrayListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    val REQUIRED_PERMISSIONS_GALLERY_PERMISSION  = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            arrayListOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            arrayListOf(
                Manifest.permission.READ_MEDIA_IMAGES
            )
        }
        else -> {
            arrayListOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }


    object DB{
        const val USERS = "users"
        const val GROUPS = "groups"
        const val NOTIFICATIONS = "notifications"
        const val MESSAGES = "Messages"
        const val COMPANY = "companies"
        const val SERVICE = "services"
    }

    object TXT_MSG_TYPE{
        const val SINGLE_MSG_BY_CURRENT_USER = 11
        const val FIRST_MSG_BY_CURRENT_USER = 22
        const val MIDDLE_MSG_BY_CURRENT_USER = 33
        const val LAST_MSG_BY_CURRENT_USER = 44

        const val SINGLE_MSG_RIGHT = 189
        const val FIRST_MSG_RIGHT = 289
        const val MIDDLE_MSG_RIGHT = 389
        const val LAST_MSG_RIGHT = 489

        const val SINGLE_MSG_LEFT = 123
        const val FIRST_MSG_LEFT = 223
        const val MIDDLE_MSG_LEFT = 323
        const val LAST_MSG_LEFT = 423
    }
}