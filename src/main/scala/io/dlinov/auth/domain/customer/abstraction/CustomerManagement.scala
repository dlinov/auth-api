package io.dlinov.auth.domain.customer.abstraction

trait CustomerManagement
    extends CustomerRead
    with CustomerUpdate
    with CustomerActivation
    with CustomerRegistration {}
