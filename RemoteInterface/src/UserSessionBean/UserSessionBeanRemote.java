/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package UserSessionBean;

import java.math.BigDecimal;
import java.util.List;
import javax.ejb.Remote;

/**
 *
 * @author Viesturs
 */
@Remote
public interface UserSessionBeanRemote {
    public String getFullUserName();
    public BigDecimal getUserDeposit();
    public String logIn(String fName, String lName);
    public String register(String fName, String lName);
    public String create(String firstName, String lastName, BigDecimal desposit);
    public Object[][] getAllUsers(String searchString);
    public String delete(int id); 
    public String update (int id, String fName, String lName, BigDecimal desposit);
   
    public Object[][] getAllInventory(String searchString);
    public String createInventory(String desc, BigDecimal price, int instock);
    public String deleteInventory(int id);
    public String updateInventory(int id, String desc, BigDecimal price, int instock);
    
    public Object[][] getInventoryByPrice(BigDecimal min, BigDecimal max);
    public Object[][] getInventoryByStockReserve();
    
    public Object[][] getAllOrders(String searchString);
    public String createOrder(int inventoryID, int amount);
    public String updateOrder(int orderID, int amt);
    public String deleteOrder(int orderID);  
    public String deleteAllUserOrders();
    public BigDecimal getUserCartSum();
    public BigDecimal checkout();
}
