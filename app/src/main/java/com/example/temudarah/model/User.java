package com.example.temudarah.model;

import com.google.firebase.firestore.GeoPoint;

public class User {
    private String uid;
    private String email;
    private String username;
    private String fullName;
    private String ktpNumber;
    private String address;
    private String birthDate;
    private String gender;
    private String bloodType;
    private String hasDonatedBefore;
    private int weight;
    private int height;
    private String profileImageBase64;
    private String lastDonationDate;
    private GeoPoint location; // Untuk menyimpan koordinat lat/lng
    private String geohash;
    private String fcmToken;

    public User() {}

    public User(String uid, String email, String username, String fullName, String ktpNumber, String address, String birthDate, String gender, String bloodType, String hasDonatedBefore, int weight, int height, String profileImageBase64, String lastDonationDate, GeoPoint location, String geohash, String fcmToken) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.fullName = fullName;
        this.ktpNumber = ktpNumber;
        this.address = address;
        this.birthDate = birthDate;
        this.gender = gender;
        this.bloodType = bloodType;
        this.hasDonatedBefore = hasDonatedBefore;
        this.weight = weight;
        this.height = height;
        this.profileImageBase64 = profileImageBase64;
        this.lastDonationDate = lastDonationDate;
        this.location = location;
        this.geohash = geohash;
        this.fcmToken = fcmToken;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getKtpNumber() {
        return ktpNumber;
    }

    public void setKtpNumber(String ktpNumber) {
        this.ktpNumber = ktpNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public String getHasDonatedBefore() {
        return hasDonatedBefore;
    }

    public void setHasDonatedBefore(String hasDonatedBefore) { this.hasDonatedBefore = hasDonatedBefore; }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getProfileImageBase64() {
        return profileImageBase64;
    }

    public void setProfileImageBase64(String profileImageBase64) { this.profileImageBase64 = profileImageBase64; }

    public String getLastDonationDate() { return lastDonationDate; }

    public void setLastDonationDate(String lastDonationDate) { this.lastDonationDate = lastDonationDate; }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public String getGeohash() {
        return geohash;
    }

    public void setGeohash(String geohash) {
        this.geohash = geohash;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}