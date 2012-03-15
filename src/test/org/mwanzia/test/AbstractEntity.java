package org.mwanzia.test;

import java.io.Serializable;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import org.codehaus.jackson.annotate.JsonProperty;
import org.mwanzia.Remote;
import org.mwanzia.Transferable;

/**
 * <p>
 * Abstract base class for entities that are identified by a numeric ID and are
 * versioned for optimistic locking.
 * </p>
 * 
 * @author percy wegmann ( percy <at> karen and percy <dot> net )
 * 
 */
@MappedSuperclass
public abstract class AbstractEntity implements Serializable {
    private static final long serialVersionUID = 4302558210598736398L;

    private Long id;
    private Long version = 0L;

    /**
     * Methods that just reloads the current entity. Useful for refreshing on
     * the client-side.
     * 
     * @return
     */
    @Remote
    public AbstractEntity reload() {
        return this;
    }

    @Id
    @GeneratedValue
    @JsonProperty
    @Transferable
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Version
    @JsonProperty
    @Transferable
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractEntity other = (AbstractEntity) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

}
