package com.tvf.clb.service.service;

public enum RaceType {
    Horse {
        @Override
        public String toString() {
            return "Horse Racing";
        }
    },
    Greyhound{
        @Override
        public String toString() {
            return "Greyhound Racing";
        }
    },
    Harness{
        @Override
        public String toString() {
            return "Harness Racing";
        }
    };
}

