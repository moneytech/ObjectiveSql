/*
 * Copyright (C) 2009-2013 The Project Lombok Authors.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.braisdom.funcsql.apt;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * Represents a single annotation in a source file and can be used to query the parameters present on it.
 * 
 */
public class AnnotationValues<A extends Annotation> {
	private final Class<A> type;
	private final Map<String, AnnotationValue> values;

	/**
	 * Represents a single method on the annotation class. For example, the value() method on the Getter annotation.
	 */
	public static class AnnotationValue {
		/** A list of the raw expressions. List is size 1 unless an array is provided. */
		public final List<String> raws;
		
		/** Guesses for each raw expression. It's 'primitive' (String or primitive), an AV.ClassLiteral, an AV.FieldSelect, or an array of one of those. */
		public final List<Object> valueGuesses;
		
		/** A list of the actual expressions. List is size 1 unless an array is provided. */
		public final List<Object> expressions;
		
		private final boolean isExplicit;
		
		/**
		 * Like the other constructor, but used for when the annotation method is initialized with an array value.
		 */
		public AnnotationValue(List<String> raws, List<Object> expressions, List<Object> valueGuesses, boolean isExplicit) {
			this.raws = raws;
			this.expressions = expressions;
			this.valueGuesses = valueGuesses;
			this.isExplicit = isExplicit;
		}
		
		/** {@inheritDoc} */
		@Override
        public String toString() {
			return "raws: " + raws + " valueGuesses: " + valueGuesses;
		}
		
		public boolean isExplicit() {
			return isExplicit;
		}
	}
	
	/**
	 * Creates a new AnnotationValues.
	 * 
	 * @param type The annotation type. For example, "Getter.class"
	 * @param values a Map of method names to AnnotationValue instances, for example 'value -> annotationValue instance'.
	 */
	public AnnotationValues(Class<A> type, Map<String, AnnotationValue> values) {
		this.type = type;
		this.values = values;
	}
	
	public static <A extends Annotation> AnnotationValues<A> of(Class<A> type) {
		return new AnnotationValues<A>(type, Collections.<String, AnnotationValue>emptyMap());
	}
	
	/**
	 * Thrown on the fly if an actual annotation instance procured via the {@link #getInstance()} method is queried
	 * for a method for which this AnnotationValues instance either doesn't have a guess or can't manage to fit
	 * the guess into the required data type.
	 */
	public static class AnnotationValueDecodeFail extends RuntimeException {
		private static final long serialVersionUID = 1L;
		
		/** The index into an array initializer (e.g. if the second value in an array initializer is
		 * an integer constant expression like '5+SomeOtherClass.CONSTANT', this exception will be thrown,
		 * and you'll get a '1' for idx. */
		public final int idx;
		
		/** The AnnotationValue object that goes with the annotation method for which the failure occurred. */
		public final AnnotationValue owner;
		
		public AnnotationValueDecodeFail(AnnotationValue owner, String msg, int idx) {
			super(msg);
			this.idx = idx;
			this.owner = owner;
		}
	}
	
	private static AnnotationValueDecodeFail makeNoDefaultFail(AnnotationValue owner, Method method) {
		return new AnnotationValueDecodeFail(owner, 
				"No value supplied but " + method.getName() + " has no default either.", -1);
	}
	
	private A cachedInstance = null;
	
	/**
	 * Creates an actual annotation instance. You can use this to query any annotation methods, except for
	 * those annotation methods with class literals, as those can most likely not be turned into Class objects.
	 * 
	 * If some of the methods cannot be implemented, this method still works; it's only when you call a method
	 * that has a problematic value that an AnnotationValueDecodeFail exception occurs.
	 */
	@SuppressWarnings("unchecked")
	public A getInstance() {
		InvocationHandler invocations = new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				AnnotationValue v = values.get(method.getName());
				if (v == null) {
					Object defaultValue = method.getDefaultValue();
					if (defaultValue != null) return defaultValue;
					throw makeNoDefaultFail(v, method);
				}
				
				boolean isArray = false;
				Class<?> expected = method.getReturnType();
				Object array = null;
				if (expected.isArray()) {
					isArray = true;
					expected = expected.getComponentType();
					array = Array.newInstance(expected, v.valueGuesses.size());
				}
				
				if (!isArray && v.valueGuesses.size() > 1) {
					throw new AnnotationValueDecodeFail(v, 
						"Expected a single value, but " + method.getName() + " has an array of values", -1);
				}
				
				if (v.valueGuesses.size() == 0 && !isArray) {
					Object defaultValue = method.getDefaultValue();
					if (defaultValue == null) throw makeNoDefaultFail(v, method);
					return defaultValue;
				}
				
				int idx = 0;
				for (Object guess : v.valueGuesses) {
					Object result = guess == null ? null : guessToType(guess, expected, v, idx);
					if (!isArray) {
						if (result == null) {
							Object defaultValue = method.getDefaultValue();
							if (defaultValue == null) throw makeNoDefaultFail(v, method);
							return defaultValue;
						}
						return result;
					} 
					if (result == null) {
						if (v.valueGuesses.size() == 1) {
							Object defaultValue = method.getDefaultValue();
							if (defaultValue == null) throw makeNoDefaultFail(v, method);
							return defaultValue;
						} 
						throw new AnnotationValueDecodeFail(v, 
							"I can't make sense of this annotation value. Try using a fully qualified literal.", idx);
					}
					Array.set(array, idx++, result);
				}
				
				return array;
			}
		};
		
		return cachedInstance = (A) Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, invocations);
	}
	
	private Object guessToType(Object guess, Class<?> expected, AnnotationValue v, int pos) {
		if (expected == int.class || expected == Integer.class) {
			if (guess instanceof Integer || guess instanceof Short || guess instanceof Byte) {
				return ((Number) guess).intValue();
			}
		}
		
		if (expected == long.class || expected == Long.class) {
			if (guess instanceof Long || guess instanceof Integer || guess instanceof Short || guess instanceof Byte) {
				return ((Number) guess).longValue();
			}
		}
		
		if (expected == short.class || expected == Short.class) {
			if (guess instanceof Integer || guess instanceof Short || guess instanceof Byte) {
				int intVal = ((Number) guess).intValue();
				int shortVal = ((Number) guess).shortValue();
				if (shortVal == intVal) return shortVal;
			}
		}
		
		if (expected == byte.class || expected == Byte.class) {
			if (guess instanceof Integer || guess instanceof Short || guess instanceof Byte) {
				int intVal = ((Number) guess).intValue();
				int byteVal = ((Number) guess).byteValue();
				if (byteVal == intVal) return byteVal;
			}
		}
		
		if (expected == double.class || expected == Double.class) {
			if (guess instanceof Number) return ((Number) guess).doubleValue();
		}
		
		if (expected == float.class || expected == Float.class) {
			if (guess instanceof Number) return ((Number) guess).floatValue();
		}
		
		if (expected == boolean.class || expected == Boolean.class) {
			if (guess instanceof Boolean) return ((Boolean) guess).booleanValue();
		}
		
		if (expected == char.class || expected == Character.class) {
			if (guess instanceof Character) return ((Character) guess).charValue();
		}
		
		if (expected == String.class) {
			if (guess instanceof String) return guess;
		}

		throw new AnnotationValueDecodeFail(v,
			"Can't translate a " + guess.getClass() + " to the expected " + expected, pos);
	}
	
	/**
	 * Returns the raw expressions used for the provided {@code annotationMethodName}.
	 * 
	 * You should use this method for annotation methods that return {@code Class} objects. Remember that
	 * class literals end in ".class" which you probably want to strip off.
	 */
	public List<String> getRawExpressions(String annotationMethodName) {
		AnnotationValue v = values.get(annotationMethodName);
		return v == null ? Collections.<String>emptyList() : v.raws;
	}
	
	/**
	 * Returns the actual expressions used for the provided {@code annotationMethodName}.
	 */
	public List<Object> getActualExpressions(String annotationMethodName) {
		AnnotationValue v = values.get(annotationMethodName);
		return v == null ? Collections.<Object>emptyList() : v.expressions;
	}
	
	public boolean isExplicit(String annotationMethodName) {
		AnnotationValue annotationValue = values.get(annotationMethodName);
		return annotationValue != null && annotationValue.isExplicit();
	}
	
	/**
	 * Convenience method to return the first result in a {@link #getRawExpressions(String)} call.
	 * 
	 * You should use this method if the annotation method is not an array type.
	 */
	public String getRawExpression(String annotationMethodName) {
		List<String> l = getRawExpressions(annotationMethodName);
		return l.isEmpty() ? null : l.get(0);
	}
	
	/**
	 * Convenience method to return the first result in a {@link #getActualExpressions(String)} call.
	 * 
	 * You should use this method if the annotation method is not an array type.
	 */
	public Object getActualExpression(String annotationMethodName) {
		List<Object> l = getActualExpressions(annotationMethodName);
		return l.isEmpty() ? null : l.get(0);
	}

}