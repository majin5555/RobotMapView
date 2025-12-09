package com.siasun.dianshi.geometry;

public class TurnDir {


    public int m_tagTurnDir;      // The tag for the turn direction


    public TurnDir(TurnDirTag tagTurnDir) {
        m_tagTurnDir = 0;
    }

    // Default constructor
    public TurnDir() {
        m_tagTurnDir = TurnDirTag.COUNTER_CLOCKWISE;
    }


    public class TurnDirTag {
        private static final int COUNTER_CLOCKWISE = 0;
        private static final int CLOCKWISE = 1;
    }

}
