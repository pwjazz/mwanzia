package org.mwanzia;

/**
 * Pretty prints JavaScript.
 * 
 * @author percy
 * 
 */
public class PrettyPrinter {
    private int indentation = 0;
    private StringBuilder builder = new StringBuilder();

    /**
     * Increase the indentation level by 1. This does not actually write
     * anything - indentation is written on the next call to newline().
     * 
     * @return
     */
    public PrettyPrinter indent() {
        this.indentation += 1;
        return this;
    }

    /**
     * Decrease the indentation level by 1. This does not actually write
     * anything - indentation is written on the next call to newline().
     * 
     * @return
     */
    public PrettyPrinter outdent() {
        this.indentation -= 1;
        return this;
    }

    /**
     * Adds a newline and indents to the current indentation level.
     * 
     * @return
     */
    public PrettyPrinter newline() {
        builder.append("\n");
        for (int i = 0; i < indentation; i++) {
            builder.append("  ");
        }
        return this;
    }

    /**
     * Prints the specified object as text (using toString()).
     * 
     * @param object
     * @return
     */
    public PrettyPrinter print(Object object) {
        builder.append(object);
        return this;
    }

    /**
     * Same as print().
     * 
     * @param object
     * @return
     */
    public PrettyPrinter write(Object object) {
        return print(object);
    }

    /**
     * Returns all text printed to this point.
     */
    public String toString() {
        return builder.toString();
    }
}
