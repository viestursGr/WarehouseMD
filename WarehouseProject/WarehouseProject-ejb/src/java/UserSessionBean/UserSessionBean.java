/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package UserSessionBean;

import WarehouseEntities.Inventory;
import WarehouseEntities.Orders;
import WarehouseEntities.Users;
import WarehouseSessionBean.WarehouseSessionBeanLocal;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author Viesturs
 */
@Stateful
public class UserSessionBean implements UserSessionBeanRemote {
    @EJB
    private WarehouseSessionBeanLocal warehouseSessionBean;
    
    @PersistenceContext(unitName = "WarehouseProject-ejbPU")
    private EntityManager em;     
    private Users currentUser;
    
    @Override
    public String logIn(String firstName, String lastName) {        
        if (isEmpty(firstName) || isEmpty(lastName)) {
            return "Incorrect first or last name!";
        }
        
        if(userExists(firstName.trim(), lastName.trim())){
            currentUser = getUserByNames(firstName.trim(), lastName.trim());
            return null;
        }
        
        return "User does not exist!";
    }

    @Override
    public String register(String firstName, String lastName) {
        if (isEmpty(firstName) || isEmpty(lastName)) {
            return "Incorrect first or last name!";
        }
        
        if(userExists(firstName.trim(), lastName.trim())){
            return "User already exists!";
        }
        
        currentUser = new Users(lastUserIdentifier() + 1);
        currentUser.setFirstName(firstName.trim());
        currentUser.setLastName(lastName.trim());
        currentUser.setDeposit(BigDecimal.ZERO);
        
        em.persist(currentUser);
        System.out.println("User is registered!");
        
        return null;
    }
    
    @Override
    public String create(String firstName, String lastName, BigDecimal deposit) {
        if (isEmpty(firstName) || isEmpty(lastName)) {
            return "Incorrect first or last name!";
        }
        
        if(userExists(firstName.trim(), lastName.trim())){
            return "User already exists!";
        }
        
        if(deposit.compareTo(BigDecimal.ZERO) < 0){
            return "Starting deposit can't be negative!";
        }
        
        Users user = new Users(lastUserIdentifier() + 1);
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setDeposit(deposit);
        
        em.persist(user);
        System.out.println("New user is created!");
        
        return null;
    }
    
    @Override
    public String getFullUserName() {
        Users user = em.find(Users.class, this.currentUser.getId());
        return  user.getFirstName() + " " + user.getLastName();
    }
    
    @Override
    public Object[][] getAllUsers(String searchString) {
         List<Users> users = getAllUsersList(searchString);
         Object[][] results = mapUserList(users);
         return results;
    }

    // Deletes user. Can't delete currently logged in user.
    @Override
    public String delete(int id)   {
        if(userExists(id) == false) {
            return "No user found!";
        }
        
        Users user = em.find(Users.class, id);
        if(!user.equals(currentUser)) {
            
            List<Orders> orders = user.getOrdersList();
            
            if(isEmpty(orders) == false){
                for(Orders o: orders){
                    em.remove(o);
                }
            }
            
            em.remove(user);
            return null;
        }
        
        return "User is logged in!";
    }

    @Override
    public String update(int id, String firstName, String lastName, BigDecimal deposit) {
        Users user = em.find(Users.class, id);
        
        if (user != null) {           
            if
            (
                userExists(firstName, lastName) && 
                (firstName.equals(user.getLastName()) == false && 
                lastName.equals(user.getLastName()) == false)
            ){
                return "User already exists!";
            }            
            
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setDeposit(deposit);

            em.persist(user);
            
            System.out.println("User: " + firstName+ " " + lastName + " updated!");
            return null;
        }
            
        return "No user found!";
    }

    @Override
    public Object[][] getAllInventory(String searchString) {
         List<Inventory> inventoryList = getAllInventoryList(searchString);
         Object[][] result = mapInventoryList(inventoryList);
         return result;
    }

    @Override
    public String createInventory(String description, BigDecimal price, int instock) {
        if(
                price == null ||
                price.compareTo(BigDecimal.ZERO) == -1
           ){
            return "Price must be less than zero!";
        }        
        
        if(instock < 0){
            return "Inventory amount must be positive or zero!";
        }
        
        Inventory inventory = new Inventory(lastInventoryIdentifier() + 1);    

        inventory.setDescription(description);
        inventory.setPrice(price);
        inventory.setInstock(instock);
        inventory.setReserved(0);
        em.persist(inventory);
        
        return null;
    }

    @Override
    public String deleteInventory(int id) {
        Inventory inventory  = em.find(Inventory.class, id);
        if (inventory != null) {
            List<Orders> orders = inventory.getOrdersList();
            
            if(isEmpty(orders) == false){
                for(Orders o: orders){
                    em.remove(o);
                }
            }
            
            em.remove(inventory);
            return null;
        }
        
        return "No inventory found!";
    }

    @Override
    public String updateInventory(int id, String description, BigDecimal price, int instock) {
        Inventory inventory = em.find(Inventory.class, id);
        
        if(
                price == null ||
                price.compareTo(BigDecimal.ZERO) == -1
           ){
            return "Price must be less than zero!";
        }        
        
        if(instock < 0){
            return "Inventory amount must be positive or zero!";
        }
        
        if (inventory != null) {
            inventory.setDescription(description);
            inventory.setPrice(price);
            int stockDifference = instock - inventory.getInstock();

            inventory.setInstock(instock);
            
            if(stockDifference < 0){
                List<Orders> orders = getAllInventoryOrders(inventory);
                
                if(isEmpty(orders) == false) {
                    sortOderList(orders);

                    for(Orders order: orders){
                        int orderAmount = order.getAmount();
                        stockDifference += orderAmount;

                        if(stockDifference >= 0){
                            orderAmount = stockDifference;
                        }

                        if(stockDifference < 0 || orderAmount == 0){
                            em.remove(order);
                        } else {
                           order.setAmount(orderAmount);
                           em.persist(order);
                           break;
                        }

                        if(stockDifference >= 0){
                            break;
                        }
                    }                    
                }
            }
            
            em.persist(inventory);
            return null;
        }
        
        return "No inventory found!";
    }


    @Override
    public Object[][] getInventoryByPrice(BigDecimal min, BigDecimal max) {
        List<Inventory> inventoryList = warehouseSessionBean.getByPriceRange(min, max);
        Object[][] results = mapInventoryList(inventoryList);
        return results;
    }

    @Override
    public Object[][] getInventoryByStockReserve() {
        List<Inventory> inventoryList = warehouseSessionBean.getByStockReserve();
        Object[][] results = mapInventoryList(inventoryList);
        return results;
    }

    @Override
    public String createOrder(int inventoryID, int amount) {
        int userID = currentUser.getId();
        Inventory inventory = em.find(Inventory.class, inventoryID);

        if(inventory == null){
            return "No inventory found!";
        }
        
        if(amount <= 0){
            return "Only positive amount allowed!";
        }
        
        int free = inventory.getInstock() - inventory.getReserved();
        
        if(free == 0) {
            return "No inventory in stock!";
        }
        
        amount = free > amount ? amount : free;
        
        List<Orders> orders = this.getUserOrdersList(null);
        boolean exists = false;
        
        Orders order = new Orders(lastOrderIdentifier()+1);
        order.setItemId(em.find(Inventory.class, inventoryID));
        order.setUserId(currentUser);
        order.setAmount(amount);
        order.setDate(new Date());
        em.persist(order);
    
        Inventory inventory2 = em.find(Inventory.class, inventoryID);
        inventory2.setReserved(inventory2.getReserved() + amount);
        em.persist(inventory2);
        
        return null;
    }

    @Override
    public Object[][] getAllOrders(String searchString) {
        List <Orders> orders = getUserOrdersList(searchString);
        Object[][] results = mapOrderList(orders);
        return results;
    }

    @Override
    public String deleteOrder(int orderID) {
        Orders order = em.find(Orders.class, orderID);
        if(order != null) {
            Inventory inventory = order.getItemId();
            inventory.setReserved(inventory.getReserved() - order.getAmount());
            em.persist(inventory);
            em.remove(order);
            return null;
        }
        
        return "No order found!";
    }
    
    @Override
    public String deleteAllUserOrders() {
        List<Orders> orders = this.getUserOrdersList(null);
       if(isEmpty(orders)){
           return null;
       } 
       
       for(Orders order: orders){
           Inventory inventory = em.find(Inventory.class, order.getItemId().getId());
           inventory.setReserved(inventory.getReserved() - order.getAmount());
           em.persist(inventory);
           em.remove(order);
       }
       
       return null;
    }
    
    @Override
    public String updateOrder(int orderID, int amount) {        
        if(amount < 0){
            return "Only positive amount allowed!";
        }
        
        List<Orders> orders = this.getUserOrdersList(null);

        for(Orders order: orders){
            if(order.getId() == orderID){
                Inventory inventory = order.getItemId();
                
                if(amount == 0){
                    return this.deleteOrder(orderID);
                }

                int difference = amount - order.getAmount();
                
                if(difference == 0){
                    return null;
                } else if(difference > 0) {
                    int free = inventory.getInstock() - inventory.getReserved();
                    amount = free > amount ? amount : free;
                    inventory.setReserved(inventory.getReserved() + amount);
                    order.setAmount(amount);
                } else {
                    inventory.setReserved(inventory.getReserved() - difference);
                    order.setAmount(amount);
                }
                
                em.persist(order);
                em.persist(inventory);
                return null;           
            }
        }
        
        return "No order found!";
    }

    @Override
    public BigDecimal getUserCartSum() {
        List<Orders> orders = getUserOrdersList(null);
        BigDecimal sum = BigDecimal.ZERO;
        
        if(isEmpty(orders)){
            return sum;
        }
        
        for(Orders order: orders){
            sum = sum.add(order.getItemId().getPrice().multiply(BigDecimal.valueOf(order.getAmount())));
        }
        
        return sum;
    }

    @Override
    public BigDecimal checkout() {
        BigDecimal sum = getUserCartSum();   
        Users user = em.find(Users.class, currentUser.getId());
        List<Orders> orders = this.getUserOrdersList(null);
        
        if(isEmpty(orders)){
            return sum;
        }

        for(Orders order: orders){
            Inventory inventory = order.getItemId();
            inventory.setReserved(inventory.getReserved() - order.getAmount());
            inventory.setInstock(inventory.getInstock() - order.getAmount());
            em.persist(inventory);
            em.remove(order);
        }       
        
        user.setDeposit(user.getDeposit().min(sum));
        em.persist(user);
        
        return sum;
    }
    
    @Override
    public BigDecimal getUserDeposit() {
        return em.find(Users.class, this.currentUser.getId()).getDeposit();
    }
    
    // Private methods
    public void persist(Object object) {
        em.persist(object);
    }
    
    private List<Users> getAllUsersList(String searchString) {
        Query query =  isEmpty(searchString) 
                ? em.createNamedQuery("Users.findAll") 
                : em.createQuery("SELECT u FROM Users u WHERE u.firstName LIKE '%" + searchString.trim() + "%' OR u.lastName LIKE '%" + searchString.trim() + "%'");

        return query.getResultList();
    }  
    
    private Users getUserByNames(String firstName, String lastName){
        Query query = em.createQuery("SELECT u FROM Users u WHERE u.firstName = :firstName AND u.lastName = :lastName");
        query.setParameter("firstName", firstName);
        query.setParameter("lastName", lastName);
        List<Users> users = query.getResultList();
        return users != null && users.size() > 0 ? users.get(0) : null;
    }
    
    private List<Inventory> getAllInventoryList(String searchString) {
        Query query =  isEmpty(searchString) 
                        ? em.createNamedQuery("Inventory.findAll") 
                        : em.createQuery("SELECT i FROM Inventory i WHERE i.description LIKE '%" + searchString.trim() + "%'");
        
        return query.getResultList();
    }
    
    private List<Orders> getAllInventoryOrders(Inventory inventory) {
        Query query = em.createQuery("SELECT o FROM Orders o WHERE o.itemId = :itemId");
        query.setParameter("itemID", inventory);
        return query.getResultList();
    }
    
    private List<Orders> getAllOrdersList(String searchString) {
        Query query =  isEmpty(searchString) 
                ? em.createNamedQuery("Orders.findAll")
                : em.createQuery("SELECT o FROM Orders o WHERE (o.userId = :userId) AND "
                        + "(o.userId.firstName LIKE '%" + searchString.trim() + "%' OR "
                        + "o.userId.lastName LIKE '%" + searchString.trim() + "%' OR "
                        + "o.itemId.description LIKE '%" + searchString.trim() + "%')");
        
        return query.getResultList();
    }
    
    private List<Orders> getUserOrdersList(String searchString){
        Query query = isEmpty(searchString) 
                ? em.createQuery("SELECT o FROM Orders o WHERE o.userId = :userId")
                : em.createQuery("SELECT o FROM Orders o WHERE (o.userId = :userId) AND "
                        + "(o.userId.firstName LIKE '%" + searchString.trim() + "%' OR "
                        + "o.userId.lastName LIKE '%" + searchString.trim() + "%' OR "
                        + "o.itemId.description LIKE '%" + searchString.trim() + "%')");
        
        query.setParameter("userId", currentUser);
        List<Orders> orders = query.getResultList();
        sortOderList(orders);
        return orders;
    }
    
    private boolean userExists(int id){
        Query query = em.createNamedQuery("Users.findById");
        query.setParameter("id", id);
        List<Users> users = query.getResultList();
        return users != null && users.size() > 0;
    }
    
    private boolean userExists(String firstName, String lastName){
        return getUserByNames(firstName, lastName) != null;
    }
    
    // Gets last user identifier
    private int lastUserIdentifier() {
        List<Users> users = getAllUsersList(null);
        int max = 0;
        
        if(isEmpty(users) == false){
            for (Users user : users) {
                if(user.getId() > max) {
                    max = user.getId();
                }
            }
        }
        
        return max;
    }    
    
    private int lastInventoryIdentifier() {
        List<Inventory> inventories = getAllInventoryList(null);
        int max = 0;
        
        if(isEmpty(inventories) == false){
            for (Inventory inventory : inventories) {
                if(inventory.getId() > max) {
                    max = inventory.getId();
                }
            }    
        }
        
        return max;
    }
    
    private int lastOrderIdentifier() {
        List<Orders> orders = getAllOrdersList(null);
        int max = 0;

        if(isEmpty(orders) == false){
           for (Orders order : orders) {
                if(order.getId() > max) { 
                    max = order.getId();
                }
            }         
        }
        
        return max;
    }
    
    private boolean isEmpty(String text){
        return text == null || text.trim().equals("");
    }
    
    private boolean isEmpty(List list){
        return list == null || list.size() <= 0;
    }
    
    private Object[][] mapInventoryList(List<Inventory> inventoryList){
        if(isEmpty(inventoryList)){
            return null;
        }
        
        Object[][] results = new Object[inventoryList.size()][5];
        int j = 0;
        for (Inventory inventory: inventoryList) {
            results[j][0] = inventory.getId();
            results[j][1] = inventory.getDescription();
            results[j][2] = inventory.getPrice();
            results[j][3] = inventory.getInstock();
            results[j][4] = inventory.getReserved();
            j++;
        }
        
        return results;
    }
    
    private Object[][] mapUserList(List<Users> users){
        if(isEmpty(users)){
            return null;
        }
        
         Object[][] results = new Object[users.size()][4];
         int i = 0;
         for (Users user: users) {
             results[i][0] = user.getId();
             results[i][1] = user.getFirstName();
             results[i][2] = user.getLastName();
             results[i][3] = user.getDeposit();
             i++;
         }
         
         return results;
    }
    
    private Object[][] mapOrderList(List<Orders> orders){
        if(isEmpty(orders)){
            return null;
        }        
        
        Object[][] result = new Object[orders.size()][5];
        int j = 0;
        for (Orders order : orders){
            result[j][0] =  order.getId();
            result[j][1] =  order.getItemId().getId();
            result[j][2] =  order.getAmount();
            result[j][3] =  order.getItemId().getPrice().multiply(new BigDecimal(order.getAmount()));
            result[j][4] =  order.getDate();
            j++;
        }
        
        return result;
    }

    private void sortOderList(List<Orders> orders) {
        if(isEmpty(orders))
                return;
        
        Collections.sort(orders, new Comparator<Orders>(){
            @Override
            public int compare(Orders o1, Orders o2) {
                return o1.getDate() != null ? o1.getDate().compareTo(o2.getDate()) : -1;
            }
        });        
    }
}
