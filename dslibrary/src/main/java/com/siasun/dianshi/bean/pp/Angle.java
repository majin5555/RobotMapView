package com.siasun.dianshi.bean.pp;


import android.annotation.SuppressLint;

import com.siasun.dianshi.bean.Point2d;

import java.io.Serializable;

public class Angle implements Serializable {

    //public enum ANGLE_MODE {IN_RADIAN, IN_DEGREE}

    private static final int IN_RADIAN = 0;
    private static final int IN_DEGREE = 1;

    public static float m_fReso = (float) (Math.PI / 180);     // Default resolution is 1 degree.

    public float m_fRad;                // The angle value in radian


    public Angle(float fValue) {
        int nMode = 0;
        Angle(fValue, nMode);
    }

    //
    //Angle: The constructor.
    //
    @SuppressLint("NotConstructor")
    public void Angle(float fValue, int nMode) {
        // Convert into radian if neccessary
        m_fRad = (nMode == IN_RADIAN) ? fValue : ToRadian(fValue);

        // Normalize the angle
        m_fRad = NormAngle(m_fRad);
    }

    //
    //���������㹹��ǡ�
    //
    public Angle(Point2d pt1, Point2d pt2) {
        // Convert into radian if neccessary
        m_fRad = (float) Math.atan2(pt2.y - pt1.y, pt2.x - pt1.x);

        // Normalize the angle
        m_fRad = NormAngle(m_fRad);
    }

    //
    //Degree: Get the angle value in "degree".
    //
    public float Degree() {
        return ToDegree(NormAngle());
    }

    //
    //Degree: Get the angle value in "degree".
    //
    public float Degree2() {
        return ToDegree(NormAngle2());
    }

    //
    //Quadrant: Return the angle's Quadrant. (1/2/3/4).
    //
    public int Quadrant() {
        if (m_fRad < Math.PI / 2)                    // Quadrant 1
            return 1;
        else if (m_fRad < Math.PI)                 // Quadrant 2
            return 2;
        else if (m_fRad < 3 * Math.PI / 2)             // Quadrant 3
            return 3;
        else                                  // Quadrant 4
            return 4;
    }

    //
    //ToRadian: Convert a degree angle into a radian value.
    //
    public float ToRadian(float fDeg) {
        return (float) ((Math.PI / 180) * fDeg);
    }

    //
    //ToDegree: Convert a radian angle into a degree value.
    //
    public float ToDegree(float fRad) {
        return (float) ((180 / Math.PI) * fRad);
    }

    //
    //NormAngle: Normalize an radian angle into the range [0, 2*Math.PI).
    //
    public static float NormAngle(float fRad) {
        // Scale the angle into [0, +)
        while (fRad < 0)
            fRad += 2 * Math.PI;

        // Scale the angle into [0, 2*Math.PI)
        while (fRad >= 2 * Math.PI)
            fRad -= 2 * Math.PI;

        return fRad;
    }

    //
    //NormAngle: Normalize an radian angle into the range [-Math.PI, Math.PI).
    //
    public float NormAngle2(float fRad) {
        // Scale the angle into [0, +)
        while (fRad < -Math.PI)
            fRad += 2 * Math.PI;

        // Scale the angle into [0, 2*Math.PI)
        while (fRad >= Math.PI)
            fRad -= 2 * Math.PI;

        return fRad;
    }

    //
    //NormAngle: Normalize the angle into the range [0, 2 * Math.PI).
    //
    public float NormAngle() {
        float fRad = m_fRad;

        // Scale the angle into [0, +)
        while (fRad < 0)
            fRad += 2 * Math.PI;

        // Scale the angle into [0, 2 * Math.PI)
        while (fRad >= 2 * Math.PI)
            fRad -= 2 * Math.PI;

        return fRad;
    }

    //
    //NormAngle: Normalize the angle into the range [-Math.PI, Math.PI).
    //
    public float NormAngle2() {
        float fRad = m_fRad;

        // Scale the angle into [-Math.PI, +)
        while (fRad < -Math.PI)
            fRad += 2 * Math.PI;

        // Scale the angle into [-Math.PI, Math.PI)
        while (fRad >= Math.PI)
            fRad -= 2 * Math.PI;

        return fRad;
    }

    //
    //SetAngleReso: Set the resolution (in radian) of angle comparison.
    //
    float SetReso(float fReso) {
        // Save old resolution
        float fTemp = m_fReso;

        // Set new resolution
        m_fReso = fReso;

        // Return the old reso value
        return fTemp;
    }

    //
    //������תһ���Ƕȡ�
    //
    public void Rotate(float fAng) {
        m_fRad = NormAngle(m_fRad + fAng);
    }

    //	//
//	//Operator "-": Return the negation of the angle.
//	//
//	Angle operator -() final
//	{
//		return Angle(-m_fRad);
//	}
//	
//	//
//	//Operator "!": Return the reverse-directioned angle.
//	//
//	Angle operator !() final
//	{
//		return Angle(m_fRad + Math.PI);
//	}
//	
//	//
//	//Operator "+": Return the sum of 2 angles.
//	//
//	Angle operator +(final Angle& Ang) final
//	{
//		return Angle(m_fRad + Ang.m_fRad);
//	}
//	
//	//
//	//Operator "-": Return the difference of 2 angles.
//	//
//	Angle operator -(final Angle& Ang) final
//	{
//		return Angle(m_fRad - Ang.m_fRad);
//	}
//	
//	//
//	//Operator "+=": Increment of angle.
//	//
//	void operator +=(final Angle& Ang)
//	{
//		m_fRad = NormAngle(m_fRad + Ang.m_fRad);
//	}
//	
//	//
//	//Operator "-=": Decrement of angle.
//	//
//	void operator -=(final Angle& Ang)
//	{
//		m_fRad = NormAngle(m_fRad - Ang.m_fRad);
//	}
//	
//	//
//	//Operator "==": Test if the 2 given angles are equal.
//	//
//	//NOTE: 
//	//If the difference of the 2 angles is less than the "resolution",
//	//the 2 angles will be regarded as equal.
//	//
//	boolean operator ==(final Angle& Ang) final
//	{
//		float fTemp = (float)fabs(m_fRad - Ang.m_fRad);
//		return (fTemp < m_fReso || 2*Math.PI - fTemp < m_fReso);
//	}
//	
//	//
//	//Operator "!=": Test if the 2 given angles are not equal.
//	//
//	boolean operator !=(final Angle& Ang) final
//	{
//		return !(*this == Ang);
//	}
//	
//	//
    //�ж��������Ƿ������ȡ�
    //
    public boolean ApproxEqualTo(final Angle Ang, float fMaxDiffRad) {
        if (fMaxDiffRad == 0)
            fMaxDiffRad = m_fReso;

        float fTemp = (float) Math.abs(m_fRad - Ang.m_fRad);
        return (fTemp < fMaxDiffRad || 2 * Math.PI - fTemp < fMaxDiffRad);
    }

    //	//
//	//Operator ">": Test if the first angle is bigger than the second one.
//	//
//	boolean operator >(final Angle& Ang) final
//	{
//		return ((m_fRad > Ang.m_fRad) && (*this != Ang));
//	}
//	
//	//
//	//Operator "<": Test if the first angle is smaller than the second one.
//	//
//	boolean operator <(final Angle& Ang) final
//	{
//		return ((m_fRad < Ang.m_fRad) && (*this != Ang));
//	}
//	
//	//
//	//Operator ">=": Test if the first angle is bigger than or equal to
//	//the second one.
//	//
//	boolean operator >=(final Angle& Ang) final
//	{
//		return ((m_fRad > Ang.m_fRad) || (*this == Ang));
//	}
//	
//	//
//	//Operator "<=": Test if the first angle is smaller than or equal to
//	//the second one.
//	//
//	boolean operator <=(final Angle& Ang) final
//	{
//		return ((m_fRad < Ang.m_fRad) || (*this == Ang));
//	}
//	
//	//
//	//Operator "=": Set the angle value.
//	//
//	void operator =(float fRad)
//	{
//		m_fRad = NormAngle(fRad);
//	}
//	
//	//
//	//Operator "+": Return the sum of 2 angles.
//	//
//	Angle operator +(float fAngle) final
//	{
//		return Angle(m_fRad + fAngle);
//	}
//	
//	//
//	//Operator "-": Return the difference of 2 angles.
//	//
//	Angle operator -(float fAngle) final
//	{
//		return Angle(m_fRad - fAngle);
//	}
//	
    public Angle Subtract(Angle Ang1, Angle Ang2) {
        return new Angle(Ang1.m_fRad - Ang2.m_fRad);
    }
//	//
//	//Operator "+=": Increment of angle.
//	//
//	void operator +=(float fAngle)
//	{
//		m_fRad = NormAngle(m_fRad + fAngle);
//	}
//	
//	//
//	//Operator "-=": Decrement of angle.
//	//
//	void operator -=(float fAngle)
//	{
//		m_fRad = NormAngle(m_fRad - fAngle);
//	}
//	
//	//
//	//Operator "==": Check whether the angle is equal to the specified radian value.
//	//
//	boolean operator ==(float fRad) final
//	{
//		float fTemp = (float)fabs(m_fRad - NormAngle(fRad));
//		return (fTemp < m_fReso || 2*Math.PI - fTemp < m_fReso);
//	}
//	
//	boolean operator !=(float fAngle) final
//	{
//		return !(*this == fAngle);
//	}
//	
//	boolean operator >(float fAngle) final
//	{
//		fAngle = NormAngle(fAngle);
//		return ((m_fRad > fAngle) && (*this != fAngle));	
//	}
//	
//	boolean operator <(float fAngle) 
//	{
//		fAngle = NormAngle(fAngle);
//		return ((m_fRad < fAngle) && (*this != fAngle));
//	}
//	
//	boolean operator >=(float fAngle) 
//	{
//		fAngle = NormAngle(fAngle);
//		return ((m_fRad > fAngle) || (*this == fAngle));
//	}
//	
//	boolean operator <=(float fAngle)  
//	{
//		fAngle = NormAngle(fAngle);
//		return ((m_fRad < fAngle) || (*this == fAngle));
//	}

    //
    //���㱾��������һ���ǵĲ�(ֻ������ֵ)�����������Ϊ�����ڵĽǣ�
    //ֻ����������С�Ĳ�ֵ��
    //
    float GetDifference(final Angle another) {
        float ang1 = NormAngle(m_fRad);
        float ang2 = NormAngle(another.m_fRad);

        if (ang1 > ang2) {
            if (ang1 - ang2 > Math.PI)
                return (float) (2 * Math.PI + ang2 - ang1);
            else
                return ang1 - ang2;
        } else if (ang1 < ang2) {
            if (ang2 - ang1 > Math.PI)
                return (float) (2 * Math.PI + ang1 - ang2);
            else
                return ang2 - ang1;
        } else
            return 0;
    }

    //
    //�жϱ����Ƿ��ڴ�ang1��ang2(��ʱ����ת)�ķ�Χ֮�ڡ�
    //
    public boolean InRange(final Angle ang1, final Angle ang2) {
        Angle angDiff2 = Subtract(ang2, ang1);
        Angle angDiff1 = Subtract(this, ang1);
        return angDiff2.m_fRad > angDiff1.m_fRad;
    }

    //////////////////////////////////////////////////////////////////////////////
    //The followings are some overloaded functions: sin(), cos(), tan()

    //
    //sin: Overloaded function of sin().
    //
    public float sin(final Angle Ang) {
        return (float) Math.sin((double) Ang.m_fRad);
    }

    //
    //cos: Overloaded function of cos().
    //
    public float cos(final Angle Ang) {
        return (float) Math.cos((double) Ang.m_fRad);
    }

    //
    //tan: Overloaded function of tan().
    //
    public float tan(final Angle Ang) {
        return (float) Math.tan((double) Ang.m_fRad);
    }

    //
    //Get the absolute value of an angle. (the absolute value of -30 degree is 30 degree)
    //
    public Angle abs(final Angle Ang) {
        Angle ang = Ang;
        if (ang.m_fRad > Math.PI)
            ang.m_fRad = (float) (2 * Math.PI - ang.m_fRad);

        return ang;
    }

    float AngleDiff(float angle1, float angle2) {
        if (angle1 > 10000.0 || angle1 < -10000.0) {

            angle1 = (float) (Math.PI / 3);
        }

        if (angle2 > 10000.0 || angle2 < -10000.0) {

            angle2 = (float) (Math.PI / 3);
        }


        while (angle1 < 0) angle1 += 2 * Math.PI;
        while (angle1 > 2 * Math.PI) angle1 -= 2 * Math.PI;
        while (angle2 < 0) angle2 += 2 * Math.PI;
        while (angle2 > 2 * Math.PI) angle2 -= 2 * Math.PI;
        float diff = (float) Math.abs(angle1 - angle2);
        if (diff <= Math.PI)
            return diff;
        else
            return (float) (2 * Math.PI - diff);
    }
}
