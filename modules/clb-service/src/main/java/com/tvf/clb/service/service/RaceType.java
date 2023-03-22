package com.tvf.clb.service.service;

public enum RaceType {
    HORSE {
        @Override
        public String toString() {
            return "Horse Racing";
        }
    },
    GREYHOUND {
        @Override
        public String toString() {
            return "Greyhound Racing";
        }
    },
    HARNESS {
        @Override
        public String toString() {
            return "Harness Racing";
        }
    };
}

