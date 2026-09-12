package com.example.courslens.repository;

import com.example.courslens.domain.Page;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface PageRepository extends JpaRepository<Page,UUID> {
 List<Page> findByDocumentIdOrderByPageNumber(UUID documentId);
 @Query("select p from Page p join fetch p.document where p.document.id=:documentId and p.pageNumber=:pageNumber")
 Optional<Page> findWithDocumentByDocumentIdAndPageNumber(UUID documentId,int pageNumber);
}
