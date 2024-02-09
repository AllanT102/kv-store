package main;

/**
 * Interface representing a simple database operations contract.
 */
public interface IDatabase {

    /**
     * Retrieves data from the database based on the specified criteria.
     */
    public void get();

    /**
     * Sets or updates data in the database.
     */
    public void set();

    /**
     * Checks if a particular data entry exists in the database.
     *
     * @return True if the data exists, false otherwise.
     */
    public boolean exist();

    /**
     * Deletes a specified data entry from the database.
     */
    public void delete();
}
