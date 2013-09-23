package ca.corefacility.bioinformatics.irida.repositories.relational;

import ca.corefacility.bioinformatics.irida.exceptions.EntityExistsException;
import ca.corefacility.bioinformatics.irida.exceptions.EntityNotFoundException;
import ca.corefacility.bioinformatics.irida.exceptions.InvalidPropertyException;
import ca.corefacility.bioinformatics.irida.model.Project;
import ca.corefacility.bioinformatics.irida.model.Role;
import ca.corefacility.bioinformatics.irida.model.User;
import ca.corefacility.bioinformatics.irida.model.joins.Join;
import ca.corefacility.bioinformatics.irida.model.joins.impl.ProjectUserJoin;
import ca.corefacility.bioinformatics.irida.repositories.UserRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 
 * @author Thomas Matthews <thomas.matthews@phac-aspc.gc.ca>
 */
public class UserRelationalRepository extends GenericRelationalRepository<User> implements UserRepository {

	private static final Pattern USER_CONSTRAINT_PATTERN;

	static {
		String regex = "^USER_(.*)_CONSTRAINT$";
		USER_CONSTRAINT_PATTERN = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
	}

	public UserRelationalRepository() {
	}

	public UserRelationalRepository(DataSource source, SessionFactory sessionFactory) {
		super(source, sessionFactory, User.class);
	}

	/**
	 * {@inheritDoc }
	 */
	@Transactional
	@Override
	public User create(User object) throws IllegalArgumentException {
		Session session = this.sessionFactory.getCurrentSession();

		Role role = object.getSystemRole();
		Criteria roleQuery = session.createCriteria(Role.class);
		roleQuery.add(Restrictions.like("name", role.getName()));
		Role retRole = (Role) roleQuery.uniqueResult();
		if (retRole == null) {
			throw new IllegalArgumentException("Role " + role.getName()
					+ " doesn't exist in the database.  User cannot be created");
		}

		object.setSystemRole(retRole);

		User create;
		try {
			create = super.create(object);
		} catch (ConstraintViolationException ex) {
			String constraintName = ex.getConstraintName();
			if (StringUtils.isEmpty(constraintName)) {
				throw new EntityExistsException(
						"Could not create user as a duplicate fields exists; however the duplicate field was not included in "
								+ "the ConstraintViolationException, the original cause is included.");
			}
			Matcher matcher = USER_CONSTRAINT_PATTERN.matcher(constraintName);
			if (matcher.groupCount() != 1) {
				throw new IllegalArgumentException("Couldn't parse field name from constraint violation: "
						+ constraintName);
			}

			matcher.find();
			String fieldName = matcher.group(1).toLowerCase();

			throw new EntityExistsException("Could not create user as a duplicate field exists: " + fieldName,
					fieldName);
		}

		return create;
	}

	/**
	 * {@inheritDoc }
	 */
	@Override
	public User loadUserByUsername(String username) throws EntityNotFoundException {
		Session session = this.sessionFactory.getCurrentSession();
		Criteria crit = session.createCriteria(User.class);
		crit.add(Restrictions.like("username", username));
		User u = (User) crit.uniqueResult();

		if (u == null) {
			throw new EntityNotFoundException("User " + username + " doesn't exist in the database");
		}

		return u;
	}

	/**
	 * {@inheritDoc}
	 */
	public User getUserByUsername(String username) throws EntityNotFoundException {
		return loadUserByUsername(username);
	}

	/**
	 * {@inheritDoc }
	 */
	@Override
	public Collection<Join<Project, User>> getUsersForProject(Project project) {
		Session session = sessionFactory.getCurrentSession();

		Criteria crit = session.createCriteria(ProjectUserJoin.class);
		crit.add(Restrictions.eq("project", project));
		crit.createCriteria("user").add(Restrictions.eq("enabled", true));
		@SuppressWarnings("unchecked")
		List<Join<Project, User>> list = crit.list();

		return list;
	}

	/**
	 * {@inheritDoc }
	 */
	@Override
	public List<User> getUsersAvailableForProject(Project project) {
		Session session = sessionFactory.getCurrentSession();
		String qs = "SELECT * from user WHERE id NOT IN (select u.id from project_user p INNER JOIN user u ON p.user_id=u.id WHERE p.project_id=:pid) AND enabled=1";

		Query setLong = session.createSQLQuery(qs).addEntity(User.class).setLong("pid", project.getId());
		@SuppressWarnings("unchecked")
		List<User> list = setLong.list();

		return list;
	}

	/**
	 * Update a {@link User} object. This override checks if the User.systemRole
	 * field is being updated, and will get the appropriate persisted instance
	 * if necessary.
	 * 
	 * @param id
	 *            The ID of the user object to update
	 * @param updatedFields
	 *            The fields on the object to update
	 * @return The updated {@link User}
	 * @throws InvalidPropertyException
	 */
	@Override
	public User update(Long id, Map<String, Object> updatedFields) throws InvalidPropertyException {

		if (updatedFields.containsKey("systemRole")) {
			Session session = this.sessionFactory.getCurrentSession();
			Role role = (Role) updatedFields.get("systemRole");
			Criteria roleQuery = session.createCriteria(Role.class);
			roleQuery.add(Restrictions.like("name", role.getName()));
			Role retRole = (Role) roleQuery.uniqueResult();
			if (retRole == null) {
				throw new IllegalArgumentException("Role " + role.getName()
						+ " doesn't exist in the database.  User cannot be updated");
			}
			updatedFields.put("systemRole", retRole);
		}

		return super.update(id, updatedFields); // To change body of generated
												// methods, choose Tools |
												// Templates.
	}

}
