package com.example.temudarah.model;

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

    public User() {}

    public User(String uid, String email, String username, String fullName, String ktpNumber, String address, String birthDate, String gender, String bloodType, String hasDonatedBefore, int weight, int height) {
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

    public void setHasDonatedBefore(String hasDonatedBefore) {
        this.hasDonatedBefore = hasDonatedBefore;
    }

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
}