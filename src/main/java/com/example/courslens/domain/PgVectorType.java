package com.example.courslens.domain;

import com.pgvector.PGvector;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import java.io.Serializable;
import java.sql.*;
import java.util.Arrays;

/** Native pgvector mapping; no text embedding shadow column is retained. */
public class PgVectorType implements UserType<float[]> {
 @Override public int getSqlType(){return Types.OTHER;} @Override public Class<float[]> returnedClass(){return float[].class;}
 @Override public boolean equals(float[] a,float[] b){return Arrays.equals(a,b);} @Override public int hashCode(float[] value){return Arrays.hashCode(value);}
 @Override public float[] nullSafeGet(ResultSet rs,int position,SharedSessionContractImplementor session,Object owner)throws SQLException{Object value=rs.getObject(position);if(value==null)return null;if(value instanceof PGvector vector)return vector.toArray();return new PGvector(value.toString()).toArray();}
 @Override public void nullSafeSet(PreparedStatement statement,float[] value,int index,SharedSessionContractImplementor session)throws SQLException{if(value==null)statement.setNull(index,Types.OTHER);else statement.setObject(index,new PGvector(value));}
 @Override public float[] deepCopy(float[] value){return value==null?null:value.clone();} @Override public boolean isMutable(){return true;}
 @Override public Serializable disassemble(float[] value){return deepCopy(value);} @Override public float[] assemble(Serializable cached,Object owner){return deepCopy((float[])cached);}
}
