package org.mwanzia.test;

import org.hibernate.cfg.ImprovedNamingStrategy;

/**
 * <p>
 * Variant of ImprovedNamingStrategy that includes _id at the end of foreign key
 * column.
 * </p>
 * 
 * <p>
 * Courtesy of <a
 * href="http://matthew.mceachen.us/blog/hibernate-naming-strategies-20.html"
 * >Matthew McEachen</a>.
 * </p>
 * 
 * @author matthew.mceachan
 * 
 */
public class NewAndImprovedNamingStrategy extends ImprovedNamingStrategy {
    private static final long serialVersionUID = 5608010006329121620L;

    @Override
    public String foreignKeyColumnName(String propertyName,
            String propertyEntityName, String propertyTableName,
            String referencedColumnName) {
        String s = super.foreignKeyColumnName(propertyName, propertyEntityName,
                propertyTableName, referencedColumnName);
        return s.endsWith("_id") ? s : s + "_id";
    }
}