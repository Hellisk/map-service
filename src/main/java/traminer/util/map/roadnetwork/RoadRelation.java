package traminer.util.map.roadnetwork;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A relation in the road network graph (OSM Relation).
 * <p>
 * The relation object only stores the metadata about
 * its members. Use the attribute 'ref' to retrieve
 * the actual object from a RoadNetworkGraph object.
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class RoadRelation extends RoadNetworkPrimitive {
    /**
     * The list of members in this relation (role, type, ref)
     */
    private List<Member> members = new ArrayList<>(1);

    /**
     * Creates and empty road relation.
     *
     * @param id Relation identifier.
     */
    public RoadRelation(String id) {
        super(id);
    }

    /**
     * Creates and empty road relation.
     *
     * @param id        Relation identifier.
     * @param timeStamp Relation time-stamp.
     */
    public RoadRelation(String id, String timeStamp) {
        super(id, timeStamp);
    }

    /**
     * @return The list of members in this relation (role, type, ref).
     */
    public List<Member> getMembers() {
        return members;
    }

    /**
     * Adds all members in the list to this road relation.
     *
     * @param members The list of members to add.
     */
    public void setMembers(Collection<Member> members) {
        this.members.addAll(members);
    }

    /**
     * Adds a member to this road relation.
     *
     * @param role The member's role attribute.
     * @param type The member's type attribute.
     * @param ref  The member's reference attribute.
     */
    public void addMember(String role, String type, String ref) {
        members.add(new Member(role, type, ref));
    }

    /**
     * Adds a member to this road relation.
     *
     * @param member The member to add.
     */
    public void addMember(Member member) {
        if (member != null) members.add(member);
    }

    /**
     * The size of this road relation.
     *
     * @return Number of members in this relation.
     */
    public int size() {
        return members.size();
    }

    /**
     * Makes an exact copy of this object
     */
    @Override
    public RoadRelation clone() {
        RoadRelation clone = new RoadRelation(getId(), getTimeStamp());
        for (Member m : members) {
            clone.addMember(m.role, m.type, m.ref);
        }
        return clone;
    }

    @Override
    public String toString() {
        String s = getId() + "";
        for (Member mm : members) {
            s += " ( TYPE " + mm.type + " REF " + mm.ref + " ROLE " + mm.role + " )";
        }
        s += " )";
        return s;
    }

    @Override
    public void print() {
        System.out.println("ROAD RELATION ( " + toString() + " )");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof RoadRelation))
            return false;
        RoadRelation other = (RoadRelation) obj;
        if (this.size() != other.size())
            return false;
        return this.getId().equals(other.getId());
    }

    /**
     * A immutable class representing a Member of a road relation.
     * <p>
     * As specified in the OSM format, a relation member is
     * composed of the member's Role, Type, and Reference.
     */
    public final class Member implements Serializable {
        private final String role;
        private final String type;
        private final String ref;

        /**
         * Creates a new road relation member.
         *
         * @param role Member's role attribute.
         * @param type Member's type attribute.
         * @param ref  Member's reference attribute.
         */
        public Member(String role, String type, String ref) {
            this.role = role;
            this.type = type;
            this.ref = ref;
        }

        public String getRole() {
            return role;
        }

        public String getType() {
            return type;
        }

        public String getRef() {
            return ref;
        }
    }
}
