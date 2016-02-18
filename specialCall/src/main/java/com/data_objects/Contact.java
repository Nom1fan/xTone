package com.data_objects;

/**
 * Created by Mor on 18/02/2016.
 */
public class Contact {

    private String _name;
    private String _phoneNumber;


    public Contact(String _name, String _phoneNumber) {
        this._name = _name;
        this._phoneNumber = _phoneNumber;
    }


    public String get_name() {
        return _name;
    }

    public String get_phoneNumber() {
        return _phoneNumber;
    }
}
